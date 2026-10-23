package com.delitracker.app.places

import android.util.Log
import com.delitracker.app.BuildConfig
import com.delitracker.app.data.Restaurant
import com.delitracker.app.route.LatLon
import com.delitracker.app.route.distanceMeters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Restaurantes cercanos desde la Places API de Geoapify (datos de OpenStreetMap).
 *
 * - La clave sale de local.properties (GEOAPIFY_API_KEY) vía BuildConfig; nunca se escribe en el código.
 * - Una llamada por búsqueda. Caché en memoria: no se repite si el usuario sigue a menos de 200 m del
 *   punto anterior y la respuesta tiene menos de 10 minutos (el plan gratuito da 3.000 créditos por día).
 * - Devuelve null si no hay clave, falla la red o la respuesta no sirve; la pantalla usa entonces el catálogo de ejemplo.
 * - OpenStreetMap no trae calificaciones ni fotos: eso lo aportan las reseñas de la propia app (Firebase).
 * - Atribución obligatoria: se muestra "Geoapify · © colaboradores de OpenStreetMap" bajo el mapa.
 */
object GeoapifyPlacesRepository {

    private const val TAG = "GeoapifyPlaces"
    private const val ENDPOINT = "https://api.geoapify.com/v2/places"
    private const val DEFAULT_RADIUS_METERS = 2000
    private const val LIMIT = 20
    private const val CACHE_RADIUS_METERS = 200.0
    private const val CACHE_MAX_AGE_MS = 10 * 60 * 1000L

    private class Cached(val center: LatLon, val savedAt: Long, val places: List<Restaurant>)

    private var cached: Cached? = null

    val isConfigured: Boolean get() = BuildConfig.GEOAPIFY_API_KEY.isNotBlank()

    suspend fun nearbyRestaurants(center: LatLon, radiusMeters: Int = DEFAULT_RADIUS_METERS): List<Restaurant>? {
        if (!isConfigured) return null
        synchronized(this) {
            val hit = cached
            if (hit != null &&
                System.currentTimeMillis() - hit.savedAt < CACHE_MAX_AGE_MS &&
                distanceMeters(center, hit.center) < CACHE_RADIUS_METERS
            ) {
                return hit.places
            }
        }
        val places = withContext(Dispatchers.IO) { fetch(center, radiusMeters) } ?: return null
        synchronized(this) { cached = Cached(center, System.currentTimeMillis(), places) }
        return places
    }

    private fun fetch(center: LatLon, radiusMeters: Int): List<Restaurant>? {
        // Geoapify usa el orden lon,lat en filter y bias
        val query = "categories=catering.restaurant" +
            "&filter=circle:${center.lon},${center.lat},$radiusMeters" +
            "&bias=proximity:${center.lon},${center.lat}" +
            "&limit=$LIMIT&lang=es&apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
        val connection = URL("$ENDPOINT?$query").openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                // 401/403: clave inválida · 429: se pasó de la cuota diaria o de 5 solicitudes por segundo
                Log.w(TAG, "Geoapify respondió HTTP $code")
                return null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parse(body)
        } catch (e: IOException) {
            Log.w(TAG, "Sin conexión con Geoapify: ${e.javaClass.simpleName}")
            null
        } catch (e: JSONException) {
            Log.w(TAG, "Respuesta de Geoapify no válida")
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(body: String): List<Restaurant> {
        val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
        val places = ArrayList<Restaurant>()
        for (i in 0 until features.length()) {
            val props = features.optJSONObject(i)?.optJSONObject("properties") ?: continue
            val name = props.optString("name").trim()
            val id = props.optString("place_id").trim().replace('/', '_')
            if (name.isEmpty() || id.isEmpty() || !props.has("lat") || !props.has("lon")) continue

            // Los datos de contacto de OpenStreetMap pueden venir arriba o dentro de datasource.raw
            val raw = props.optJSONObject("datasource")?.optJSONObject("raw")
            val contact = props.optJSONObject("contact")
            fun text(vararg sources: Pair<JSONObject?, String>): String? =
                sources.firstNotNullOfOrNull { (obj, key) -> obj?.optString(key)?.trim()?.takeIf { it.isNotEmpty() } }

            places.add(
                Restaurant(
                    id = id,
                    position = LatLon(props.optDouble("lat"), props.optDouble("lon")),
                    name = name,
                    cuisine = cuisineOf(props, raw),
                    address = text(props to "address_line2", props to "formatted"),
                    phone = text(contact to "phone", props to "phone", raw to "phone", raw to "contact:phone"),
                    website = text(props to "website", contact to "website", raw to "website", raw to "contact:website"),
                    openingHours = text(props to "opening_hours", raw to "opening_hours")
                )
            )
        }
        return places
    }

    /** Tipo de cocina: el valor "cuisine" de OSM o, si no hay, la categoría más específica (catering.restaurant.italian). */
    private fun cuisineOf(props: JSONObject, raw: JSONObject?): String? {
        val fromRaw = raw?.optString("cuisine")?.split(';', ',')?.firstOrNull()?.trim().orEmpty()
        val fromCategories = props.optJSONArray("categories")?.let { array ->
            (0 until array.length()).map { array.optString(it) }
                .firstOrNull { it.startsWith("catering.restaurant.") }
                ?.removePrefix("catering.restaurant.")
        }.orEmpty()
        val cuisine = fromRaw.ifEmpty { fromCategories }
        return cuisine.replace('_', ' ').replace('-', ' ').trim()
            .replaceFirstChar { it.uppercase() }
            .ifEmpty { null }
    }
}
