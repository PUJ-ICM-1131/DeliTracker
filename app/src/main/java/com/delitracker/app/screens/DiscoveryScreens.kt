package com.delitracker.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delitracker.app.R
import com.delitracker.app.components.DtCaption
import com.delitracker.app.components.DtChipRow
import com.delitracker.app.components.DtPillRow
import com.delitracker.app.components.DtRow
import com.delitracker.app.state.AppViewModel
import com.delitracker.app.ui.theme.DtTheme

// Historia #16 · Búsqueda por nombre
@Composable
fun BuscarScreen(onAbrirDetalle: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(DtTheme.colors.surface2)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = DtTheme.colors.textMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sushi_ten), fontSize = 13.sp)
        }

        DtRow(onClick = onAbrirDetalle) {
            Text(stringResource(R.string.sushi_ten_), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(stringResource(R.string._0_9_km_4_7), fontSize = 11.sp, color = DtTheme.colors.textMuted)
        }
        DtRow(onClick = onAbrirDetalle) {
            Text(stringResource(R.string.sushi_ten_express), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(stringResource(R.string._1_4_km_4_3), fontSize = 11.sp, color = DtTheme.colors.textMuted)
        }

        DtCaption(stringResource(R.string.b_squedas_recientes))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, contentDescription = null, tint = DtTheme.colors.textMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.pizza_cerca_de_m), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, contentDescription = null, tint = DtTheme.colors.textMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.caf_con_wifi), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
        }
    }
}

// Historia #4 · Filtrar por cocina, precio, dieta y valoración
@Composable
fun FiltrosScreen(vm: AppViewModel, onAplicar: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        DtCaption(stringResource(R.string.tipo_de_cocina))
        DtChipRow(
            options = listOf(stringResource(R.string.italiana),
                stringResource(R.string.mexicana),
                stringResource(R.string.vegana), stringResource(R.string.sushi),
                stringResource(R.string.sin_gluten)
            ),
            isSelected = { vm.cuisine[it] == true },
            onToggle = { vm.toggleCuisine(it) }
        )
        DtCaption(stringResource(R.string.precio))
        DtPillRow(options = listOf(stringResource(R.string.precio_bajo),
            stringResource(R.string.precio_medio), stringResource(R.string.precio_alto)
        ), selected = vm.price, onSelect = { vm.price = it })
        DtCaption(stringResource(R.string.valoraci_n_m_nima))
        DtPillRow(options = listOf(stringResource(R.string._3), stringResource(R.string._4),
            stringResource(R.string._4_5)
        ), selected = vm.rating, onSelect = { vm.rating = it })
        DtCaption(stringResource(R.string.dieta))
        DtChipRow(
            options = listOf(stringResource(R.string.sin_gluten), stringResource(R.string.vegetariana)),
            isSelected = { vm.diet[it] == true },
            onToggle = { vm.toggleDiet(it) }
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onAplicar, modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)) {
            Text(stringResource(R.string.aplicar_filtros))
        }
    }
}
