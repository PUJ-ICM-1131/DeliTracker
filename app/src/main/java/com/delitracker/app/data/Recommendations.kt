package com.delitracker.app.data

import com.delitracker.app.route.LatLon
import com.delitracker.app.sensors.TransportMode

/** Radio máximo que se recomienda cuando el usuario va caminando. */
private const val WALKING_RADIUS_METERS = 1500f

/**
 * Recomendaciones según el modo de transporte (historia #11):
 *  - Caminando: los más cercanos (dentro de 1.5 km; si no hay ninguno, todos).
 *  - En vehículo: primero los que tienen parqueadero, y luego por distancia.
 *  - Detenido: por distancia.
 * Sin ubicación se devuelve el orden original.
 */
fun recommend(restaurants: List<Restaurant>, mode: TransportMode, user: LatLon?): List<Restaurant> {
    if (user == null) {
        return if (mode == TransportMode.VEHICLE) restaurants.sortedByDescending { it.hasParking } else restaurants
    }
    val byDistance = restaurants.sortedBy { it.geoFrom(user).meters }
    return when (mode) {
        TransportMode.STILL -> byDistance
        TransportMode.WALKING -> {
            val near = byDistance.filter { it.geoFrom(user).meters <= WALKING_RADIUS_METERS }
            if (near.isEmpty()) byDistance else near
        }
        TransportMode.VEHICLE -> byDistance.sortedByDescending { it.hasParking } // sortedBy es estable
    }
}
