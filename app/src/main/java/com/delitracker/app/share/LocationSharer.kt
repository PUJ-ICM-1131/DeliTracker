package com.delitracker.app.share

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.delitracker.app.route.LatLon
import com.delitracker.app.route.hasLocationPermission
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Lecturas de ubicación mientras se comparte (cada 5 s o 5 m). Solo funciona con la app abierta:
 * no hay servicio en segundo plano, así que al minimizar la app Android reduce o detiene las lecturas.
 */
class LocationSharer(context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private var callback: LocationCallback? = null

    /** false si falta el permiso. */
    @SuppressLint("MissingPermission") // se verifica con hasLocationPermission()
    fun start(onFix: (LatLon) -> Unit): Boolean {
        if (!hasLocationPermission(appContext)) return false
        stop()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onFix(LatLon(it.latitude, it.longitude)) }
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        return try {
            client.requestLocationUpdates(request, cb, Looper.getMainLooper())
            callback = cb
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun stop() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }
}
