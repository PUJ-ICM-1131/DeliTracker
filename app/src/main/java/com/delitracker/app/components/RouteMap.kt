package com.delitracker.app.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.delitracker.app.route.LatLon
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

private class FrameState { var key: Any? = null }

/**
 * Mapa OpenStreetMap con: marcador de origen, marcador de destino y la
 * polilínea de la ruta. Un toque en el mapa cambia el destino.
 */
@Composable
fun RouteMap(
    origin: LatLon?,
    destination: LatLon,
    route: List<LatLon>,
    originLabel: String,
    destinationLabel: String,
    onMapTap: (LatLon) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnTap by rememberUpdatedState(onMapTap)
    val frame = remember { FrameState() }

    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            map.overlays.clear()

            map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    currentOnTap(LatLon(p.latitude, p.longitude))
                    return true
                }
                override fun longPressHelper(p: GeoPoint): Boolean = false
            }))

            if (route.size >= 2) {
                map.overlays.add(Polyline().apply {
                    setPoints(route.map { GeoPoint(it.lat, it.lon) })
                    outlinePaint.color = 0xFFFF5A36.toInt()
                    outlinePaint.strokeWidth = 12f
                })
            }
            origin?.let { o ->
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(o.lat, o.lon)
                    title = originLabel
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }
            map.overlays.add(Marker(map).apply {
                position = GeoPoint(destination.lat, destination.lon)
                title = destinationLabel
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })

            // Encuadrar solo cuando cambia origen/destino/ruta (no en cada recomposición)
            val key = listOf(origin, destination, route.size)
            if (frame.key != key) {
                frame.key = key
                val points = if (route.size >= 2) route else listOfNotNull(origin, destination)
                map.post {
                    if (points.size >= 2) {
                        map.zoomToBoundingBox(
                            BoundingBox.fromGeoPoints(points.map { GeoPoint(it.lat, it.lon) }),
                            false, 120
                        )
                    } else {
                        map.controller.setCenter(GeoPoint(destination.lat, destination.lon))
                    }
                }
            }
            map.invalidate()
        }
    )
}
