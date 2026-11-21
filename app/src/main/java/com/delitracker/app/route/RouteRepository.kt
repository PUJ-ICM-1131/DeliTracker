package com.delitracker.app.route

import androidx.annotation.StringRes
import com.delitracker.app.BuildConfig
import com.delitracker.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class TravelProfile(val servicePath: String, val geoapifyMode: String) {
    WALK("routed-foot", "walk"),
    CAR("routed-car", "drive")
}

sealed interface RouteOutcome {
    data class Success(val route: RouteResult) : RouteOutcome
    data class Failure(@StringRes val messageRes: Int) : RouteOutcome
}

/**
 * Pide la ruta a Geoapify Routing (si hay GEOAPIFY_API_KEY) y, si falla o no hay clave, a un servidor
 * OSRM público de demostración (datos de OpenStreetMap, sin API key).
 */
object RouteRepository {

    suspend fun fetchRoute(origin: LatLon, destination: LatLon, profile: TravelProfile): RouteOutcome {
        if (BuildConfig.GEOAPIFY_API_KEY.isNotBlank()) {
            val outcome = fetchGeoapify(origin, destination, profile)
            if (outcome is RouteOutcome.Success) return outcome
        }
        return fetchOsrm(origin, destination, profile)
    }

    /** Geoapify usa waypoints "lat,lon|lat,lon" y devuelve GeoJSON con la geometría como MultiLineString [lon, lat]. */
    private suspend fun fetchGeoapify(origin: LatLon, destination: LatLon, profile: TravelProfile): RouteOutcome =
        withContext(Dispatchers.IO) {
            val url = URL(
                "https://api.geoapify.com/v1/routing?waypoints=${origin.lat},${origin.lon}|${destination.lat},${destination.lon}" +
                    "&mode=${profile.geoapifyMode}&apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
            )
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext RouteOutcome.Failure(R.string.route_error_server)
                }
                parseGeoapify(connection.inputStream.bufferedReader().use { it.readText() })
            } catch (e: IOException) {
                RouteOutcome.Failure(R.string.route_error_network)
            } catch (e: JSONException) {
                RouteOutcome.Failure(R.string.route_error_server)
            } finally {
                connection.disconnect()
            }
        }

    private fun parseGeoapify(body: String): RouteOutcome {
        val feature = JSONObject(body).optJSONArray("features")?.optJSONObject(0)
            ?: return RouteOutcome.Failure(R.string.route_error_no_route)
        val props = feature.getJSONObject("properties")
        val coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates")
        val points = ArrayList<LatLon>()
        fun addPoint(pair: org.json.JSONArray) {
            val point = LatLon(lat = pair.getDouble(1), lon = pair.getDouble(0)) // GeoJSON = [lon, lat]
            if (points.lastOrNull() != point) points.add(point)
        }
        for (i in 0 until coordinates.length()) {
            val item = coordinates.getJSONArray(i)
            // MultiLineString: cada elemento es una lista de puntos; LineString: cada elemento es un punto
            if (item.length() > 0 && item.optJSONArray(0) != null) {
                for (j in 0 until item.length()) addPoint(item.getJSONArray(j))
            } else if (item.length() >= 2) {
                addPoint(item)
            }
        }
        if (points.size < 2) return RouteOutcome.Failure(R.string.route_error_no_route)
        return RouteOutcome.Success(RouteResult(points, props.getDouble("distance"), props.getDouble("time")))
    }

    private suspend fun fetchOsrm(origin: LatLon, destination: LatLon, profile: TravelProfile): RouteOutcome =
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
