package com.delitracker.app.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.delitracker.app.reviews.summary
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.delitracker.app.components.RouteMap
import com.delitracker.app.components.rememberPhotoPicker
import com.delitracker.app.route.LatLon
import com.delitracker.app.route.RouteOutcome
import com.delitracker.app.route.RouteRepository
import com.delitracker.app.route.distanceMeters
import com.delitracker.app.route.TravelProfile
import com.delitracker.app.route.hasLocationPermission
import com.delitracker.app.route.requestCurrentLocation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delitracker.app.R
import com.delitracker.app.components.DtChip
import com.delitracker.app.data.displayCuisine
import com.delitracker.app.data.displayName
import com.delitracker.app.state.AppViewModel
import com.delitracker.app.ui.theme.DtTheme
import com.delitracker.app.ui.theme.DtWarning

// Historia #15 · Ficha de detalle de un restaurante
@Composable
fun DetalleScreen(vm: AppViewModel, onVerAr: () -> Unit, onResena: () -> Unit, onTrazarRuta: () -> Unit) {
    val context = LocalContext.current
    val restaurant = vm.currentRestaurant
    val name = restaurant.displayName()
    val cuisine = restaurant.displayCuisine()
    val distance = vm.userLocation?.let { distanceMeters(it, restaurant.position) }
    val summary = vm.reviews.summary()

    // Las reseñas (calificación y fotos) son de los usuarios de la app y viven en Firebase
    LaunchedEffect(restaurant.id, vm.isLoggedIn) { vm.loadReviews() }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Column(modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())) {
            // Foto: la de la reseña más reciente que tenga una; si no hay, un recuadro vacío
            val heroPhoto = vm.reviews.firstOrNull { it.photo != null }?.photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DtTheme.colors.surface2),
                contentAlignment = Alignment.Center
            ) {
                if (heroPhoto != null) {
                    ReviewPhoto(heroPhoto, Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.Photo, contentDescription = null, tint = DtTheme.colors.textMuted, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(cuisine, fontSize = 12.sp, color = DtTheme.colors.textMuted)
                if (summary != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = DtWarning, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.review_promedio, summary.average, summary.count),
                            fontSize = 12.sp, color = DtTheme.colors.textMuted
                        )
                    }
                }
            }
            if (distance != null) {
                Text(formatRouteDistance(distance), fontSize = 12.sp, color = DtTheme.colors.textMuted)
            }

            // Datos de OpenStreetMap: solo se muestran los que existen
            Spacer(Modifier.height(6.dp))
            restaurant.address?.let { InfoLine(Icons.Filled.LocationOn, it) }
            restaurant.openingHours?.let { InfoLine(Icons.Filled.AccessTime, it) }
            restaurant.phone?.let { phone ->
                InfoLine(Icons.Filled.Phone, phone) {
                    openExternal(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))))
                }
            }
            restaurant.website?.let { site ->
                InfoLine(Icons.Filled.Language, site) {
                    val url = if (site.startsWith("http", ignoreCase = true)) site else "https://$site"
                    openExternal(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { vm.toggleFavorite(name) }, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (vm.isFavorite(name)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.favorito), fontSize = 11.sp)
                }
                OutlinedButton(onClick = onResena, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.StarBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.rese_a), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onVerAr, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.ver_men_con_ar), fontSize = 12.sp)
            }

            // Reseñas
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.review_titulo), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            when {
                vm.reviewsError != null -> Text(
                    stringResource(vm.reviewsError!!), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
                vm.reviewsLoading -> Text(stringResource(R.string.review_cargando), fontSize = 11.5.sp, color = DtTheme.colors.textMuted, modifier = Modifier.padding(top = 4.dp))
                vm.reviews.isEmpty() -> Text(stringResource(R.string.review_sin_resenas), fontSize = 11.5.sp, color = DtTheme.colors.textMuted, modifier = Modifier.padding(top = 4.dp))
            }
            vm.reviews.forEach { review ->
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(review.userName, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        (1..5).forEach { n ->
                            Icon(
                                if (n <= review.stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = null, tint = DtWarning, modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    if (review.text.isNotBlank()) {
                        Text(review.text, fontSize = 12.sp, color = DtTheme.colors.textMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                    review.photo?.let {
                        ReviewPhoto(
                            it,
                            Modifier
                                .padding(top = 6.dp)
                                .height(120.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                    Divider(color = DtTheme.colors.border, thickness = 0.6.dp, modifier = Modifier.padding(top = 10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(onClick = onTrazarRuta, modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)) {
            Text(stringResource(R.string.trazar_ruta))
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 3.dp)
    ) {
        Icon(icon, contentDescription = null, tint = DtTheme.colors.textMuted, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text, fontSize = 12.sp,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else DtTheme.colors.textMuted
        )
    }
}

private fun openExternal(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.review_sin_app, Toast.LENGTH_SHORT).show()
    }
}

/** Foto de una reseña (JPEG guardado en Firestore). */
@Composable
private fun ReviewPhoto(bytes: ByteArray, modifier: Modifier = Modifier) {
    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    }
}

// Historia #5 · Ruta hacia el restaurante seleccionado
@Composable
fun RutaScreen(vm: AppViewModel, onFinalizar: () -> Unit) {
    val colors = DtTheme.colors
    val context = LocalContext.current

    val restaurant = vm.currentRestaurant
    val restaurantDestination = restaurant.position
    var destination by remember { mutableStateOf(restaurantDestination) }
    var origin by remember { mutableStateOf<LatLon?>(null) }
    var locating by remember { mutableStateOf(true) }
    var loadingRoute by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<RouteOutcome?>(null) }

    fun locate() {
        locating = true
        requestCurrentLocation(context) { result ->
            origin = result
            locating = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasLocationPermission(context)) locate() else locating = false
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) locate()
        else permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    // "Bus" no existe en el servicio de rutas: se calcula como vehículo
    val profile = if (vm.travelMode == "walk") TravelProfile.WALK else TravelProfile.CAR

    // Se vuelve a pedir la ruta cuando cambia el origen, el destino o el modo de viaje
    LaunchedEffect(origin, destination, profile) {
        val from = origin
        if (from == null) {
            outcome = null
        } else {
            loadingRoute = true
            outcome = RouteRepository.fetchRoute(from, destination, profile)
            loadingRoute = false
        }
    }

    val destinationLabel = if (destination == restaurantDestination) restaurant.displayName()
    else stringResource(R.string.ruta_destino_elegido)

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        RouteMap(
            origin = origin,
            destination = destination,
            route = (outcome as? RouteOutcome.Success)?.route?.points.orEmpty(),
            originLabel = stringResource(R.string.map_mi_ubicacion),
            destinationLabel = destinationLabel,
            onMapTap = { destination = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(10.dp), color = colors.surface2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                when {
                    locating -> Text(stringResource(R.string.ruta_ubicando), fontSize = 12.sp, color = colors.textMuted)
                    origin == null -> {
                        Text(stringResource(R.string.ruta_sin_ubicacion), fontSize = 12.sp, color = colors.textMuted)
                        TextButton(onClick = {
                            if (hasLocationPermission(context)) locate()
                            else permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }) { Text(stringResource(R.string.ruta_reintentar)) }
                    }
                    loadingRoute -> Text(stringResource(R.string.ruta_calculando), fontSize = 12.sp, color = colors.textMuted)
                    outcome is RouteOutcome.Failure -> Text(
                        stringResource((outcome as RouteOutcome.Failure).messageRes),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.error
                    )
                    outcome is RouteOutcome.Success -> {
                        val route = (outcome as RouteOutcome.Success).route
                        val minutes = (route.durationSeconds / 60).roundToInt().coerceAtLeast(1)
                        val arrival = SimpleDateFormat("h:mm a", Locale.getDefault())
                            .format(Date(System.currentTimeMillis() + (route.durationSeconds * 1000).toLong()))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.ruta_minutos, minutes), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(formatRouteDistance(route.distanceMeters), fontSize = 12.sp, color = colors.textMuted)
                        }
                        Text(stringResource(R.string.ruta_llegada, arrival), fontSize = 11.5.sp, color = colors.textMuted)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DtChip(label = stringResource(R.string.a_pie), selected = vm.travelMode == "walk", onClick = { vm.travelMode = "walk" })
                    DtChip(label = stringResource(R.string.auto), selected = vm.travelMode == "auto", onClick = { vm.travelMode = "auto" })
                    DtChip(label = stringResource(R.string.bus), selected = vm.travelMode == "bus", onClick = { vm.travelMode = "bus" })
                }
                Text(stringResource(R.string.ruta_tip_toque), fontSize = 10.5.sp, color = colors.textMuted, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onFinalizar, modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)) {
            Text(stringResource(R.string.finalizar_navegacion))
        }
        TextButton(onClick = onFinalizar, modifier = Modifier.fillMaxWidth()) { Text(
            stringResource(
                R.string.cancelar
            )) }
    }
}

@Composable
private fun formatRouteDistance(meters: Double): String =
    if (meters < 1000) stringResource(R.string.ruta_metros, meters.roundToInt())
    else stringResource(R.string.ruta_km, meters / 1000.0)

// Historia #17 · Reseña y calificación
@Composable
fun ResenaScreen(vm: AppViewModel, onPublicar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.califica_tu_visita), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(vm.currentRestaurant.displayName(), fontSize = 12.sp, color = DtTheme.colors.textMuted)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { n ->
                Icon(
                    imageVector = if (n <= vm.stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = DtWarning,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { vm.stars = n }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = vm.reviewText,
            onValueChange = { vm.reviewText = it },
            placeholder = { Text(stringResource(R.string.cuentanos_como_fue_tu_experiencia)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
        val reviewPhotoPicker = rememberPhotoPicker("review") { vm.setReviewPhoto(it) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { reviewPhotoPicker.showOptions() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.agregar_foto), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
        vm.reviewPhotoPath?.takeIf { File(it).exists() }?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.agregar_foto),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
        vm.reviewMessage?.let {
            Text(stringResource(it), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.weight(1f))
        Button(
            enabled = !vm.reviewBusy,
            onClick = { vm.publishReview { ok -> if (ok) onPublicar() } },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text(stringResource(if (vm.reviewBusy) R.string.review_publicando else R.string.publicar_resena))
        }
    }
}

// Historia #6 · AR Explorer con la cámara
@Composable
fun ArScreen(onCerrar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onCerrar, modifier = Modifier.padding(8.dp)) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cerrar), tint = Color.White)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            ) {
                Text(stringResource(R.string.ensalada_cesar_320_kcal), fontSize = 10.sp, modifier = Modifier.padding(6.dp))
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 60.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            ) {
                Text(stringResource(R.string.sin_gluten_disponible), fontSize = 10.sp, modifier = Modifier.padding(6.dp))
            }
        }
        Text(
            stringResource(R.string.apunta_al_men_para_ver_informaci_n_nutricional),
            color = DtTheme.colors.textMuted,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
