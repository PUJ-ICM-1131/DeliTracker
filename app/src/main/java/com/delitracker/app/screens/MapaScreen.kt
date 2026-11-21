package com.delitracker.app.screens

import android.Manifest
import android.hardware.GeomagneticField
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delitracker.app.R
import com.delitracker.app.data.GeoInfo
import com.delitracker.app.data.Restaurant
import com.delitracker.app.data.RestaurantCatalog
import com.delitracker.app.data.displayCuisine
import com.delitracker.app.data.displayName
import com.delitracker.app.places.GeoapifyPlacesRepository
import com.delitracker.app.data.recommend
import com.delitracker.app.components.DtPillRow
import com.delitracker.app.data.geoFrom
import com.delitracker.app.location.rememberUserLocation
import com.delitracker.app.map.OsmMap
import com.delitracker.app.route.LatLon
import com.delitracker.app.route.hasLocationPermission
import com.delitracker.app.sensors.TransportMode
import com.delitracker.app.sensors.hasAccelerometer
import com.delitracker.app.sensors.isOrientationAvailable
import com.delitracker.app.sensors.rememberOrientation
import com.delitracker.app.sensors.rememberTransportMode
import com.delitracker.app.state.AppViewModel
import com.delitracker.app.ui.theme.DtTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Historia #3 · Mapa interactivo con GPS en tiempo real.
 *
 * - Localización: FusedLocationProvider con permiso en tiempo de ejecución.
 * - Mapa: OpenStreetMap (osmdroid) con marcadores de restaurantes y del usuario.
 * - Sensor 1 (acelerómetro, historia #11): detecta si el usuario está detenido, caminando o en
 *   vehículo y cambia las recomendaciones (en vehículo, primero los que tienen parqueadero).
 *   El usuario puede corregir el modo a mano.
 * - Sensor 2 (giroscopio, historia #12): orientación del celular combinada con el GPS; la flecha
 *   apunta al restaurante elegido y se resaltan los que están en el campo de visión (±30°).
 */
@Composable
fun MapaScreen(vm: AppViewModel, onBuscar: () -> Unit, onFiltros: () -> Unit, onAbrirDetalle: () -> Unit) {
    val colors = DtTheme.colors
    val context = LocalContext.current

    // --- Permiso de ubicación (se pide al entrar a la pantalla) ---
    var permissionGranted by remember { mutableStateOf(hasLocationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> permissionGranted = result.values.any { it } }
    val requestPermission = {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
    LaunchedEffect(Unit) { if (!permissionGranted) requestPermission() }

    // --- Localización y sensores ---
    val userLocation by rememberUserLocation(permissionGranted)
    val detectedMode by rememberTransportMode()
    val orientation by rememberOrientation()
    val orientationAvailable = remember { isOrientationAvailable(context) }
    val accelerometerAvailable = remember { hasAccelerometer(context) }

    // --- Restaurantes: Geoapify cerca del usuario; si no hay clave o falla, el catálogo de ejemplo ---
    var restaurants by remember { mutableStateOf(RestaurantCatalog.all) }
    var fromApi by remember { mutableStateOf(false) }
    // Cuadrícula de ~550 m: solo se vuelve a consultar si el usuario se movió de verdad
    val searchKey = userLocation?.let { Pair((it.lat * 200).roundToInt(), (it.lon * 200).roundToInt()) }
    LaunchedEffect(searchKey) {
        val here = userLocation ?: return@LaunchedEffect
        if (!GeoapifyPlacesRepository.isConfigured) return@LaunchedEffect
        val found = GeoapifyPlacesRepository.nearbyRestaurants(here)
        if (!found.isNullOrEmpty()) {
            restaurants = found
            fromApi = true
        } else {
            restaurants = RestaurantCatalog.all
            fromApi = false
        }
    }

    LaunchedEffect(userLocation) { vm.userLocation = userLocation }

    // Amigos que comparten contigo: se escuchan mientras el mapa está abierto
    DisposableEffect(Unit) {
        vm.startWatchingFriends()
        onDispose { vm.stopWatchingFriends() }
    }
    LaunchedEffect(Unit) {
        while (true) {
            vm.tickNow()
            delay(15_000)
        }
    }

    // Modo de transporte: el detectado por el acelerómetro, salvo que el usuario lo corrija
    var manualMode by remember { mutableStateOf<TransportMode?>(null) }
    val mode = manualMode ?: detectedMode

    // --- Selección de restaurante ---
    var targetId by remember { mutableStateOf<String?>(null) }
    var focus by remember { mutableStateOf<Restaurant?>(null) }
    var recenterTick by remember { mutableIntStateOf(0) }
    var suggestion by remember { mutableStateOf<Restaurant?>(null) }

    // Recomendados primero (según el modo); el resto después
    val ordered = remember(userLocation, mode) {
        val recommended = recommend(restaurants, mode, userLocation)
        recommended + restaurants.filter { it !in recommended }
    }
    val target = restaurants.firstOrNull { it.id == targetId } ?: ordered.first()
    val targetGeo: GeoInfo? = userLocation?.let { target.geoFrom(it) }

    fun select(restaurant: Restaurant) {
        targetId = restaurant.id
        focus = restaurant
    }

    // Orientación real = orientación del celular (giroscopio) corregida a norte verdadero con la
    // declinación magnética del lugar donde está el usuario (GPS).
    val declination = remember(userLocation) { declinationFor(userLocation) }
    val heading: Float? = orientation.azimuthDeg?.let { (it + declination + 360f) % 360f }

    fun isInView(restaurant: Restaurant): Boolean {
        val facing = heading ?: return false
        val location = userLocation ?: return false
        return angularDifference(restaurant.geoFrom(location).bearing, facing) <= FIELD_OF_VIEW_HALF_DEGREES
    }

    val arrowRotation: Float? = run {
        val geo = targetGeo
        if (geo == null || heading == null) null
        else (((geo.bearing - heading) % 360f) + 360f) % 360f
    }
    val inViewNames = restaurants.filter { isInView(it) }.map { it.displayName() }

    val modeStill = stringResource(R.string.map_modo_detenido)
    val modeWalking = stringResource(R.string.map_modo_caminando)
    val modeVehicle = stringResource(R.string.map_modo_vehiculo)
    val modeLabel = when (mode) {
        TransportMode.STILL -> modeStill
        TransportMode.WALKING -> modeWalking
        TransportMode.VEHICLE -> modeVehicle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Barra de búsqueda
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(colors.surface2)
                .clickable(onClick = onBuscar)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.buscar_restaurante), color = colors.textMuted, fontSize = 13.sp)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onFiltros, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.filtros))
            }
            OutlinedButton(onClick = { vm.triggerAlertToast() }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.simular_alerta))
            }
        }

        if (!permissionGranted) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accentSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.map_permiso_mensaje),
                    color = colors.accentSoftInk,
                    fontSize = 11.5.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { requestPermission() }) {
                    Text(stringResource(R.string.map_permitir))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        // Mapa real (ocupa el espacio sobrante)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            OsmMap(
                restaurants = restaurants,
                userLocation = userLocation,
                focus = focus,
                recenterTick = recenterTick,
                onRestaurantClick = { targetId = it.id },
                modifier = Modifier.fillMaxSize(),
                friends = vm.friendLocations
            )

            FilledTonalIconButton(
                onClick = { recenterTick++ },
                enabled = userLocation != null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.map_centrar))
            }

            // Sugerencia (botón "Sugerir")
            suggestion?.let { picked ->
                LaunchedEffect(picked) {
                    delay(4000)
                    suggestion = null
                }
                val pickedGeo = userLocation?.let { picked.geoFrom(it) }
                ToastCard(
                    title = stringResource(R.string.map_sugerencia_titulo),
                    body = picked.displayName() +
                        (pickedGeo?.let { " · " + formatDistance(it.meters) } ?: ""),
                    onClick = { vm.selectRestaurant(picked); onAbrirDetalle() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // Alerta simulada (historia de notificaciones geolocalizadas)
            if (vm.showAlertToast) {
                LaunchedEffect(Unit) {
                    delay(4000)
                    vm.dismissAlertToast()
                }
                ToastCard(
                    title = stringResource(R.string.delitracker),
                    body = stringResource(R.string.verde_bowl_est_a_80_m_y_coincide_con_tus_gustos_veganos),
                    onClick = {
                        vm.selectCatalogRestaurant("verde_bowl")
                        onAbrirDetalle()
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        CompassCard(
            targetName = target.displayName(),
            distanceText = targetGeo?.let { formatDistance(it.meters) },
            arrowRotation = arrowRotation,
            orientationAvailable = orientationAvailable,
            orientationReliable = orientation.reliable,
            inViewText = if (inViewNames.isEmpty()) stringResource(R.string.map_en_vista_ninguno)
            else stringResource(R.string.map_en_vista, inViewNames.joinToString()),
            onSuggest = {
                val next = ordered[(ordered.indexOfFirst { it.id == target.id } + 1) % ordered.size]
                select(next)
                suggestion = next
            },
            onDetalle = { vm.selectRestaurant(target); onAbrirDetalle() }
        )

        // Modo de transporte (acelerómetro) con corrección manual
        Spacer(Modifier.height(8.dp))
        if (accelerometerAvailable) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    DtPillRow(
                        options = listOf(modeStill, modeWalking, modeVehicle),
                        selected = modeLabel,
                        onSelect = { picked ->
                            manualMode = when (picked) {
                                modeWalking -> TransportMode.WALKING
                                modeVehicle -> TransportMode.VEHICLE
                                else -> TransportMode.STILL
                            }
                        }
                    )
                }
                if (manualMode != null) {
                    TextButton(onClick = { manualMode = null }) {
                        Text(stringResource(R.string.map_modo_auto), fontSize = 12.sp)
                    }
                }
            }
            Text(
                if (manualMode != null) stringResource(R.string.map_modo_linea_manual, modeLabel)
                else stringResource(R.string.map_modo_linea_auto, modeLabel),
                fontSize = 10.sp,
                color = colors.textMuted
            )
        } else {
            Text(stringResource(R.string.map_sin_sensores), fontSize = 10.sp, color = colors.textMuted)
        }

        if (vm.isSharing) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.share_banner),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { vm.stopSharing() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text(stringResource(R.string.detener), fontSize = 11.sp) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(if (fromApi) R.string.map_fuente_geoapify else R.string.map_fuente_ejemplo),
            fontSize = 10.sp,
            color = colors.textMuted
        )
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ordered, key = { it.id }) { restaurant ->
                val cuisine = restaurant.displayCuisine()
                val meta = userLocation?.let {
                    if (cuisine.isBlank()) formatDistance(restaurant.geoFrom(it).meters)
                    else stringResource(R.string.map_meta_cocina_distancia, cuisine, formatDistance(restaurant.geoFrom(it).meters))
                } ?: cuisine
                val selected = restaurant.id == target.id
                val inView = isInView(restaurant)
                Column(
                    modifier = Modifier
                        .width(124.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (inView) colors.accent2Soft else MaterialTheme.colorScheme.surface)
                        .border(
                            BorderStroke(1.5.dp, if (selected) colors.accentSoftInk else colors.border),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { select(restaurant) }
                        .padding(8.dp)
                ) {
                    Text(restaurant.displayName(), fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                    Text(meta, fontSize = 10.sp, color = colors.textMuted)
                    if (mode == TransportMode.VEHICLE && restaurant.hasParking) {
                        Text(stringResource(R.string.map_con_parqueadero), fontSize = 10.sp, color = colors.accentSoftInk)
                    }
                    if (inView) {
                        Text(stringResource(R.string.map_al_frente), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.accentSoftInk)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompassCard(
    targetName: String,
    distanceText: String?,
    arrowRotation: Float?,
    orientationAvailable: Boolean,
    orientationReliable: Boolean,
    inViewText: String,
    onSuggest: () -> Unit,
    onDetalle: () -> Unit
) {
    val colors = DtTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface2)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Navigation,
                contentDescription = null,
                tint = colors.accentSoftInk,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(arrowRotation ?: 0f)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.map_brujula_hacia, targetName),
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            )
            val detail = when {
                !orientationAvailable -> stringResource(R.string.map_brujula_no_disponible)
                distanceText == null -> stringResource(R.string.map_brujula_esperando)
                else -> distanceText
            }
            Text(detail, fontSize = 11.sp, color = colors.textMuted)
            if (orientationAvailable) {
                Text(inViewText, fontSize = 10.sp, color = colors.textMuted)
                if (!orientationReliable) {
                    Text(stringResource(R.string.map_calibrar), fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            TextButton(onClick = onSuggest, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(stringResource(R.string.map_sugerir), fontSize = 12.sp)
            }
            TextButton(onClick = onDetalle, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(stringResource(R.string.map_ver_detalle), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ToastCard(title: String, body: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .padding(10.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(body, fontSize = 11.sp, color = DtTheme.colors.textMuted)
            }
        }
    }
}

private const val FIELD_OF_VIEW_HALF_DEGREES = 30f

/** Diferencia angular mínima entre dos rumbos (0..180). */
private fun angularDifference(a: Float, b: Float): Float {
    val d = abs(a - b) % 360f
    return if (d > 180f) 360f - d else d
}

/** Declinación magnética (grados) para convertir el norte magnético de la brújula a norte verdadero. */
private fun declinationFor(location: LatLon?): Float {
    if (location == null) return 0f
    return GeomagneticField(
        location.lat.toFloat(),
        location.lon.toFloat(),
        0f,
        System.currentTimeMillis()
    ).declination
}

@Composable
private fun formatDistance(meters: Float): String =
    if (meters < 1000f) stringResource(R.string.map_dist_m, meters.toInt())
    else stringResource(R.string.map_dist_km, meters / 1000f)
