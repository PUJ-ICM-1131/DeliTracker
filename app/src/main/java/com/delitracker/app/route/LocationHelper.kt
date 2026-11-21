package com.delitracker.app.route

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Obtiene la ubicación actual (GPS/red). Devuelve null si no se pudo determinar. */
@SuppressLint("MissingPermission")
fun requestCurrentLocation(context: Context, onResult: (LatLon?) -> Unit) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location != null) {
                onResult(LatLon(location.latitude, location.longitude))
            } else {
                // Sin lectura fresca: probar con la última ubicación conocida
                client.lastLocation
                    .addOnSuccessListener { last -> onResult(last?.let { LatLon(it.latitude, it.longitude) }) }
                    .addOnFailureListener { onResult(null) }
            }
        }
        .addOnFailureListener { onResult(null) }
}
