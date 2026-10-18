package com.delitracker.app.sensors

import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Estados de movimiento que distingue la historia #11. */
enum class TransportMode { STILL, WALKING, VEHICLE }

/**
 * Clasifica el movimiento a partir de la magnitud del acelerómetro (m/s²).
 * Es Kotlin puro (sin Android) para poder probarlo con señales simuladas.
 *
 * Cada ventana de [windowSeconds] segundos se resume con dos números:
 *  - desviación estándar de la magnitud (cuánto se mueve el celular);
 *  - periodicidad (pico de autocorrelación a lag de pasos: 0.35 a 1.5 s). Caminar es muy periódico;
 *    la vibración de un vehículo no lo es.
 *
 *  STILL   -> desviación < 0.12
 *  WALKING -> desviación >= 0.5 y periodicidad >= 0.45
 *  VEHICLE -> cualquier otro movimiento
 *
 * Para no parpadear entre estados, el modo solo cambia cuando 2 ventanas seguidas coinciden.
 * Son umbrales heurísticos: hay que afinarlos con la prueba de campo de la historia #11.
 */
class TransportClassifier(private val windowSeconds: Float = 4f) {

    private val times = ArrayList<Long>()
    private val values = ArrayList<Float>()
    private var current = TransportMode.STILL
    private var candidate: TransportMode? = null

    /**
     * Agrega una muestra. Devuelve el modo estable cuando se completa una ventana
     * (puede ser el mismo de antes) o null si todavía se está llenando la ventana.
     */
    fun add(timestampNanos: Long, magnitude: Float): TransportMode? {
        times.add(timestampNanos)
        values.add(magnitude)
        val seconds = (timestampNanos - times.first()) / 1_000_000_000f
        if (seconds < windowSeconds) return null

        val raw = analyze(seconds)
        times.clear()
        values.clear()

        if (raw == current) {
            candidate = null
        } else if (raw == candidate) {
            current = raw
            candidate = null
        } else {
            candidate = raw
        }
        return current
    }

    fun reset() {
        times.clear()
        values.clear()
        current = TransportMode.STILL
        candidate = null
    }

    private fun analyze(seconds: Float): TransportMode {
        val n = values.size
        if (n < 8) return current
        val mean = values.sum() / n
        var variance = 0f
        for (v in values) variance += (v - mean) * (v - mean)
        variance /= n
        val std = sqrt(variance)
        if (std < STILL_STD) return TransportMode.STILL

        val rate = n / seconds // muestras por segundo reales
        val minLag = (rate * 0.35f).roundToInt().coerceAtLeast(2)
        val maxLag = (rate * 1.5f).roundToInt().coerceAtMost(n - 3)
        var best = 0f
        if (minLag <= maxLag) {
            // autocorrelación normalizada para lags (minLag-1)..(maxLag+1)
            val first = minLag - 1
            val r = FloatArray(maxLag - first + 2)
            for (k in r.indices) {
                val lag = first + k
                var sum = 0f
                for (i in 0 until n - lag) sum += (values[i] - mean) * (values[i + lag] - mean)
                r[k] = sum / ((n - lag) * variance)
            }
            // Solo cuenta un PICO local (máximo entre sus vecinos): una señal lenta y suave
            // decae de forma continua y no tiene pico en el rango de pasos.
            for (k in 1 until r.size - 1) {
                if (r[k] > r[k - 1] && r[k] >= r[k + 1] && r[k] > best) best = r[k]
            }
        }
        return if (std >= WALK_STD && best >= WALK_PERIODICITY) TransportMode.WALKING
        else TransportMode.VEHICLE
    }

    companion object {
        const val STILL_STD = 0.12f
        const val WALK_STD = 0.5f
        const val WALK_PERIODICITY = 0.45f
    }
}
