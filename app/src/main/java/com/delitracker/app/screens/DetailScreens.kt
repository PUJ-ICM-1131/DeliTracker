package com.delitracker.app.screens

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
import com.delitracker.app.state.AppViewModel
import com.delitracker.app.ui.theme.DtTheme
import com.delitracker.app.ui.theme.DtWarning

// Historia #15 · Ficha de detalle de un restaurante
@Composable
fun DetalleScreen(vm: AppViewModel, onVerAr: () -> Unit, onResena: () -> Unit, onTrazarRuta: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DtTheme.colors.surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Photo, contentDescription = null, tint = DtTheme.colors.textMuted, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.trattoria_sole), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.italiana_precios), fontSize = 12.sp, color = DtTheme.colors.textMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = DtWarning, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string._4_6), fontSize = 12.sp, color = DtTheme.colors.textMuted)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.abierto_hasta_10_00_pm), fontSize = 12.sp, color = DtTheme.colors.textMuted)
            Text(stringResource(R.string._0_4_km), fontSize = 12.sp, color = DtTheme.colors.textMuted)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { vm.toggleFavorited() }, modifier = Modifier.weight(1f)) {
                Icon(
                    if (vm.favorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
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

        Spacer(Modifier.weight(1f))
        Button(onClick = onTrazarRuta, modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)) {
            Text(stringResource(R.string.trazar_ruta))
        }
    }
}

// Historia #5 · Ruta hacia el restaurante seleccionado
@Composable
fun RutaScreen(vm: AppViewModel, onFinalizar: () -> Unit) {
    val colors = DtTheme.colors
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accentSoft)
        ) {
            Icon(
                Icons.Filled.LocationOn, contentDescription = null, tint = colors.accentSoftInk,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(28.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.accentSoftInk)
            )
        }
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(10.dp), color = colors.surface2, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string._8_min), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string._650_m), fontSize = 12.sp, color = colors.textMuted)
                }
                Text(stringResource(R.string.llegada_12_48_pm), fontSize = 11.5.sp, color = colors.textMuted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DtChip(label = stringResource(R.string.a_pie), selected = vm.travelMode == "walk", onClick = { vm.travelMode = "walk" })
                    DtChip(label = stringResource(R.string.auto), selected = vm.travelMode == "auto", onClick = { vm.travelMode = "auto" })
                    DtChip(label = stringResource(R.string.bus), selected = vm.travelMode == "bus", onClick = { vm.travelMode = "bus" })
                }
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
        Text(stringResource(R.string.trattoria_sole), fontSize = 12.sp, color = DtTheme.colors.textMuted)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.agregar_foto), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onPublicar, modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)) {
            Text(stringResource(R.string.publicar_resena))
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
