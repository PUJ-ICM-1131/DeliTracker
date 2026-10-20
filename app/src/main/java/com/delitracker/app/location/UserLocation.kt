package com.delitracker.app.location

import android.annotation.SuppressLint
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.delitracker.app.route.LatLon
import com.delitracker.app.route.hasLocationPermission
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Ubicación del usuario en tiempo real (FusedLocationProvider, el mismo que ya usa
 * route/LocationHelper.kt pero con actualizaciones continuas). Escucha solo mientras
 * el composable está en pantalla y [enabled] es true; se desregistra al salir.
 */
@SuppressLint("MissingPermission") // se verifica con hasLocationPermission() antes de registrar
@Composable
fun rememberUserLocation(enabled: Boolean): State<LatLon?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<LatLon?>(null) }

    DisposableEffect(enabled) {
        if (!enabled || !hasLocationPermission(context)) {
            return@DisposableEffect onDispose { }
        }
        val client = LocationServices.getFusedLocationProviderClient(context)

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { state.value = LatLon(it.latitude, it.longitude) }
            }
        }

        // Valor rápido mientras llega la primera lectura fresca
        client.lastLocation.addOnSuccessListener { last ->
            if (last != null && state.value == null) {
                state.value = LatLon(last.latitude, last.longitude)
            }
        }

        // cada 2 s y solo si el usuario se movió al menos 5 m
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // permiso revocado mientras tanto: simplemente no habrá actualizaciones
        }

        onDispose { client.removeLocationUpdates(callback) }
    }
    return state
}
