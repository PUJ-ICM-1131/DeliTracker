package com.delitracker.app.route

import androidx.annotation.StringRes
import com.delitracker.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class TravelProfile(val servicePath: String) {
    WALK("routed-foot"),
    CAR("routed-car")
}

sealed interface RouteOutcome {
    data class Success(val route: RouteResult) : RouteOutcome
    data class Failure(@StringRes val messageRes: Int) : RouteOutcome
}

/**
 * Pide la ruta a un servidor OSRM público (datos de OpenStreetMap, sin API key).
 * Es un servicio de demostración: para producción conviene alojar tu propio OSRM
 * o usar un proveedor con SLA (OpenRouteService, Google Directions, etc.).
 */
object RouteRepository {

    suspend fun fetchRoute(origin: LatLon, destination: LatLon, profile: TravelProfile): RouteOutcome =
        withContext(Dispatchers.IO) {
            val url = URL(
                "https://routing.openstreetmap.de/${profile.servicePath}/route/v1/driving/" +
                    "${origin.lon},${origin.lat};${destination.lon},${destination.lat}" +
                    "?overview=full&geometries=geojson"
            )
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("User-Agent", "DeliTracker/1.0 (Android)")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext RouteOutcome.Failure(R.string.route_error_server)
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parse(body)
            } catch (e: IOException) {
                RouteOutcome.Failure(R.string.route_error_network)
            } catch (e: JSONException) {
                RouteOutcome.Failure(R.string.route_error_server)
            } finally {
                connection.disconnect()
            }
        }

    private fun parse(body: String): RouteOutcome {
        val json = JSONObject(body)
        if (json.optString("code") != "Ok") return RouteOutcome.Failure(R.string.route_error_no_route)
        val route = json.getJSONArray("routes").getJSONObject(0)
        val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
        val points = List(coords.length()) { i ->
            val pair = coords.getJSONArray(i)
            LatLon(lat = pair.getDouble(1), lon = pair.getDouble(0)) // GeoJSON = [lon, lat]
        }
        return RouteOutcome.Success(
            RouteResult(points, route.getDouble("distance"), route.getDouble("duration"))
        )
    }
}
