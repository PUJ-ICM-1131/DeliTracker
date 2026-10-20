package com.delitracker.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.sqrt

/*
 * Sensor 1: ACELERÓMETRO  -> historia #11: detectar modo de transporte (detenido / caminando / vehículo).
 * Sensor 2: GIROSCOPIO    -> historia #12: orientación del dispositivo. Se usa el sensor de vector de
 *           rotación de Android, que el sistema calcula FUSIONANDO giroscopio + acelerómetro + magnetómetro;
 *           si el dispositivo no tiene giroscopio, se cae a acelerómetro + magnetómetro.
 * Ninguno es el sensor de luminosidad. Ambos se registran con SENSOR_DELAY_UI (~16 Hz, bajo consumo)
 * y se liberan al salir de la pantalla.
 */

/** Orientación del celular. [azimuthDeg]: 0 = norte magnético, 90 = este; null mientras no haya datos. */
data class Orientation(
    val azimuthDeg: Float? = null,
    /** false si el sistema reporta lectura inestable (hay que calibrar moviendo el celular en forma de 8). */
    val reliable: Boolean = true,
    /** true si la orientación viene del giroscopio (vector de rotación fusionado). */
    val usesGyroscope: Boolean = false
)

/** true si hay con qué calcular la orientación (giroscopio, o acelerómetro + magnetómetro). */
fun isOrientationAvailable(context: Context): Boolean {
    val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val rotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    val fallback = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
        manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    return rotation || fallback
}

fun hasAccelerometer(context: Context): Boolean {
    val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    return manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
}

/**
 * Modo de transporte detectado con el acelerómetro (historia #11).
 * Cada 4 s se evalúa la magnitud de la aceleración con [TransportClassifier].
 */
@Composable
fun rememberTransportMode(enabled: Boolean = true): State<TransportMode> {
    val context = LocalContext.current
    val mode = remember { mutableStateOf(TransportMode.STILL) }

    DisposableEffect(enabled) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || accelerometer == null) {
            return@DisposableEffect onDispose { }
        }
        val classifier = TransportClassifier()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                classifier.add(event.timestamp, sqrt(x * x + y * y + z * z))?.let { mode.value = it }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            manager.unregisterListener(listener)
            classifier.reset()
        }
    }
    return mode
}

/**
 * Orientación del dispositivo (historia #12). Con giroscopio usa el vector de rotación fusionado;
 * sin él, acelerómetro + magnetómetro con filtro paso-bajo. Detecta lecturas inestables.
 */
@Composable
fun rememberOrientation(enabled: Boolean = true): State<Orientation> {
    val context = LocalContext.current
    val orientation = remember { mutableStateOf(Orientation()) }

    DisposableEffect(enabled) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val hasGyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val useGyroscope = rotationSensor != null && hasGyroscope
        val useFallback = !useGyroscope && accelerometer != null && magnetometer != null
        if (!enabled || (!useGyroscope && !useFallback)) {
            return@DisposableEffect onDispose { }
        }

        val rotation = FloatArray(9)
        val remapped = FloatArray(9)
        val angles = FloatArray(3)
        var reliable = true

        fun publish(matrix: FloatArray) {
            SensorManager.getOrientation(matrix, angles)
            val azimuth = (Math.toDegrees(angles[0].toDouble()).toFloat() + 360f) % 360f
            // Solo se publica si giró al menos 1.5°: evita recomponer la pantalla ~16 veces por segundo
            val previous = orientation.value.azimuthDeg
            if (previous != null) {
                val diff = abs(azimuth - previous)
                if (minOf(diff, 360f - diff) < MIN_AZIMUTH_CHANGE_DEGREES) return
            }
            orientation.value = Orientation(
                azimuthDeg = azimuth,
                reliable = reliable,
                usesGyroscope = useGyroscope
            )
        }

        val listener: SensorEventListener
        val sensors: List<Sensor>

        if (useGyroscope) {
            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotation, event.values)
                    // Con el celular de pie (cámara al frente) el "frente" es -Z, no -Y
                    val upright = abs(rotation[8]) < 0.7f
                    if (upright) {
                        SensorManager.remapCoordinateSystem(
                            rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapped
                        )
                        publish(remapped)
                    } else {
                        publish(rotation)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    reliable = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
                    orientation.value = orientation.value.copy(reliable = reliable)
                }
            }
            sensors = listOf(rotationSensor!!)
        } else {
            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            var hasGravity = false
            var hasGeomagnetic = false
            listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            smooth(event.values, gravity, hasGravity)
                            hasGravity = true
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            smooth(event.values, geomagnetic, hasGeomagnetic)
                            hasGeomagnetic = true
                        }
                        else -> return
                    }
                    if (hasGravity && hasGeomagnetic &&
                        SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)
                    ) {
                        publish(rotation)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        reliable = accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
                        orientation.value = orientation.value.copy(reliable = reliable)
                    }
                }
            }
            sensors = listOf(accelerometer!!, magnetometer!!)
        }

        sensors.forEach { manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { manager.unregisterListener(listener) }
    }
    return orientation
}

private const val SMOOTHING = 0.15f
private const val MIN_AZIMUTH_CHANGE_DEGREES = 1.5f

/** Filtro paso-bajo: suaviza el ruido del sensor para que la flecha no tiemble. */
private fun smooth(input: FloatArray, output: FloatArray, hasPrevious: Boolean) {
    for (i in 0 until 3) {
        output[i] = if (hasPrevious) output[i] + SMOOTHING * (input[i] - output[i]) else input[i]
    }
}
