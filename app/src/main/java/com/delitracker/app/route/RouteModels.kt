package com.delitracker.app.route

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double)

data class RouteResult(
    val points: List<LatLon>,
    val distanceMeters: Double,
    val durationSeconds: Double
)

/** Coordenadas de ejemplo del restaurante (reemplazar por las reales cuando haya backend). */
object SampleRestaurants {
    val TRATTORIA_SOLE = LatLon(4.6763, -74.0483)
}

/** Distancia en línea recta entre dos puntos (fórmula de haversine), en metros. */
fun distanceMeters(a: LatLon, b: LatLon): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lon - a.lon)
    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * atan2(sqrt(h), sqrt(1 - h))
}
