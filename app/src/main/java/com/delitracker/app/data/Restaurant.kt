package com.delitracker.app.data

import androidx.annotation.StringRes
import com.delitracker.app.R
import com.delitracker.app.route.LatLon
import com.delitracker.app.route.SampleRestaurants
import com.delitracker.app.route.distanceMeters
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Restaurante de ejemplo para el mapa. Mientras no se conecte la API externa
 * (Foursquare Places), esta lista alimenta el mapa. Nombre y cocina salen de strings.xml.
 */
data class Restaurant(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val cuisineRes: Int,
    val position: LatLon,
    /** Dato de ejemplo; vendrá de la API. Se usa al recomendar cuando el usuario va en vehículo. */
    val hasParking: Boolean = false
)

/** Distancia en metros y rumbo inicial (grados, 0..360 desde el norte verdadero). */
data class GeoInfo(val meters: Float, val bearing: Float)

fun Restaurant.geoFrom(origin: LatLon): GeoInfo =
    GeoInfo(
        meters = distanceMeters(origin, position).toFloat(),
        bearing = bearingDegrees(origin, position)
    )

/** Rumbo inicial de [from] hacia [to], en grados 0..360 (0 = norte, 90 = este). */
fun bearingDegrees(from: LatLon, to: LatLon): Float {
    val lat1 = Math.toRadians(from.lat)
    val lat2 = Math.toRadians(to.lat)
    val dLon = Math.toRadians(to.lon - from.lon)
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val degrees = Math.toDegrees(atan2(y, x))
    return (((degrees % 360.0) + 360.0) % 360.0).toFloat()
}

object RestaurantCatalog {
    /** Trattoria Sole usa la misma coordenada que ya usa la pantalla de ruta. */
    val all: List<Restaurant> = listOf(
        Restaurant("trattoria_sole", R.string.trattoria_sole, R.string.italiana, SampleRestaurants.TRATTORIA_SOLE, hasParking = true),
        Restaurant("verde_bowl", R.string.verde_bowl, R.string.vegana, LatLon(4.6752, -74.0495)),
        Restaurant("sushi_ten", R.string.sushi_ten_, R.string.sushi, LatLon(4.6778, -74.0470), hasParking = true),
        Restaurant("cafe_andino", R.string.cafe_andino, R.string.map_cocina_cafe, LatLon(4.6770, -74.0500))
    )

    /** Centro inicial del mapa cuando todavía no hay ubicación del usuario. */
    val defaultCenter: LatLon = SampleRestaurants.TRATTORIA_SOLE
}
