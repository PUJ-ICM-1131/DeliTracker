package com.delitracker.app.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.delitracker.app.R
import com.delitracker.app.data.Restaurant
import com.delitracker.app.data.RestaurantCatalog
import com.delitracker.app.route.LatLon
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

private const val DEFAULT_ZOOM = 16.0
private const val FOCUS_ZOOM = 17.5

/** osmdroid necesita un User-Agent propio (política de uso de OpenStreetMap) y una caché. */
private fun configureOsmdroid(context: Context) {
    val app = context.applicationContext
    val config = Configuration.getInstance()
    config.load(app, app.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
    config.userAgentValue = app.packageName
    val base = File(app.cacheDir, "osmdroid")
    config.osmdroidBasePath = base
    config.osmdroidTileCache = File(base, "tiles")
}

/**
 * Mapa de OpenStreetMap (sin API key) con un marcador por restaurante y otro para el usuario.
 *
 * @param focus restaurante al que se debe acercar el mapa (null = no mover).
 * @param recenterTick súbelo en 1 para centrar el mapa en la ubicación del usuario.
 */
@Composable
fun OsmMap(
    restaurants: List<Restaurant>,
    userLocation: LatLon?,
    focus: Restaurant?,
    recenterTick: Int,
    onRestaurantClick: (Restaurant) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnClick by rememberUpdatedState(onRestaurantClick)

    val mapView = remember {
        configureOsmdroid(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(RestaurantCatalog.defaultCenter.lat, RestaurantCatalog.defaultCenter.lon))
        }
    }

    // El mapa debe pausarse/reanudarse con el ciclo de vida de la pantalla
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Si ya estaba en RESUMED cuando se compone, onResume no se vuelve a emitir
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Marcador del usuario: se crea una vez y solo se mueve (así no se cierran los globos de info)
    val userMarker = remember(mapView) {
        Marker(mapView).apply {
            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
            title = context.getString(R.string.map_mi_ubicacion)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            isEnabled = false
        }
    }

    // Marcadores de restaurantes
    DisposableEffect(mapView, restaurants) {
        val markers = restaurants.map { restaurant ->
            Marker(mapView).apply {
                position = GeoPoint(restaurant.position.lat, restaurant.position.lon)
                title = context.getString(restaurant.nameRes)
                snippet = context.getString(restaurant.cuisineRes)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { marker, _ ->
                    marker.showInfoWindow()
                    currentOnClick(restaurant)
                    true
                }
            }
        }
        mapView.overlays.addAll(markers)
        mapView.overlays.add(userMarker)
        mapView.invalidate()
        onDispose {
            mapView.overlays.removeAll(markers)
            mapView.overlays.remove(userMarker)
        }
    }

    // Mover el marcador del usuario cuando llega una ubicación nueva
    LaunchedEffect(userLocation) {
        val location = userLocation
        if (location != null) {
            userMarker.position = GeoPoint(location.lat, location.lon)
            userMarker.isEnabled = true
        } else {
            userMarker.isEnabled = false
        }
        mapView.invalidate()
    }

    // Primera ubicación: centrar una vez
    LaunchedEffect(userLocation != null) {
        val location = userLocation
        if (location != null) {
            mapView.controller.animateTo(GeoPoint(location.lat, location.lon))
        }
    }

    // Botón "centrar en mi ubicación"
    LaunchedEffect(recenterTick) {
        val location = userLocation
        if (recenterTick > 0 && location != null) {
            mapView.controller.setZoom(DEFAULT_ZOOM)
            mapView.controller.animateTo(GeoPoint(location.lat, location.lon))
        }
    }

    // Acercar a un restaurante elegido (tarjeta, marcador o sugerencia por agitar)
    LaunchedEffect(focus) {
        if (focus != null) {
            mapView.controller.setZoom(FOCUS_ZOOM)
            mapView.controller.animateTo(GeoPoint(focus.position.lat, focus.position.lon))
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
