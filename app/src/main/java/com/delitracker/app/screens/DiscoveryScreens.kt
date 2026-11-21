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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.saveable.rememberSaveable
import com.delitracker.app.data.Restaurant
import com.delitracker.app.data.RestaurantCatalog
import com.delitracker.app.data.displayCuisine
import com.delitracker.app.data.displayName
import com.delitracker.app.places.GeoapifyPlacesRepository
import com.delitracker.app.places.RecentSearches
import com.delitracker.app.route.distanceMeters
import kotlinx.coroutines.delay
import com.delitracker.app.R
import com.delitracker.app.components.DtCaption
import com.delitracker.app.components.DtChipRow
import com.delitracker.app.components.DtPillRow
import com.delitracker.app.components.DtRow
import com.delitracker.app.state.AppViewModel
import com.delitracker.app.ui.theme.DtTheme

// Historia #16 · Búsqueda por nombre o palabra clave
private const val MIN_QUERY = 2
private const val DEBOUNCE_MS = 450L

private sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Done(val results: List<Restaurant>, val notice: Int?) : SearchState
}

@Composable
fun BuscarScreen(vm: AppViewModel, onAbrirDetalle: () -> Unit) {
    val context = LocalContext.current
    val center = vm.userLocation ?: RestaurantCatalog.defaultCenter
    var query by rememberSaveable { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    var recents by remember { mutableStateOf(RecentSearches.load(context)) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Busca 450 ms después de la última tecla; si el texto cambia, la búsqueda anterior se cancela
    LaunchedEffect(query) {
        val text = query.trim()
        if (text.length < MIN_QUERY) {
            state = SearchState.Idle
            return@LaunchedEffect
        }
        state = SearchState.Loading
        delay(DEBOUNCE_MS)
        val online = GeoapifyPlacesRepository.searchByName(text, center)
        state = if (online != null) {
            SearchState.Done(online.sortedBy { distanceMeters(center, it.position) }, null)
        } else {
            val local = RestaurantCatalog.all.filter { it.matches(context, text) }
            SearchState.Done(
                local,
                if (GeoapifyPlacesRepository.isConfigured) R.string.buscar_sin_red else R.string.buscar_sin_clave
            )
        }
    }

    fun open(restaurant: Restaurant) {
        recents = RecentSearches.add(context, query.ifBlank { restaurant.displayName(context) })
        focusManager.clearFocus()
        vm.selectRestaurant(restaurant)
        onAbrirDetalle()
    }

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
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(stringResource(R.string.buscar_restaurante), fontSize = 13.sp, color = DtTheme.colors.textMuted)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        recents = RecentSearches.add(context, query)
                        focusManager.clearFocus()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.buscar_borrar_texto),
                        tint = DtTheme.colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when (val current = state) {
            SearchState.Idle -> {
                if (query.isNotBlank()) {
                    Text(stringResource(R.string.buscar_minimo), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
                }
                if (recents.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) { DtCaption(stringResource(R.string.b_squedas_recientes)) }
                        Text(
                            stringResource(R.string.buscar_borrar_recientes),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                RecentSearches.clear(context)
                                recents = emptyList()
                            }
                        )
                    }
                    recents.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { query = item }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.History, contentDescription = null, tint = DtTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(item, fontSize = 12.sp, color = DtTheme.colors.textMuted)
                        }
                    }
                }
            }

            SearchState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.buscar_cargando), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
            }

            is SearchState.Done -> {
                current.notice?.let {
                    Text(stringResource(it), fontSize = 11.sp, color = DtTheme.colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                }
                if (current.results.isEmpty()) {
                    Text(
                        stringResource(R.string.buscar_sin_resultados, query.trim()),
                        fontSize = 12.sp,
                        color = DtTheme.colors.textMuted
                    )
                } else {
                    DtCaption(stringResource(R.string.buscar_resultados_cantidad, current.results.size))
                    LazyColumn {
                        items(current.results, key = { it.id }) { restaurant ->
                            val meters = distanceMeters(center, restaurant.position).toFloat()
                            DtRow(onClick = { open(restaurant) }) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(restaurant.displayName(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    val detail = listOf(restaurant.displayCuisine(), restaurant.address.orEmpty())
                                        .filter { it.isNotBlank() }.joinToString(" · ")
                                    if (detail.isNotEmpty()) {
                                        Text(
                                            detail, fontSize = 11.sp, color = DtTheme.colors.textMuted,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    if (meters < 1000f) stringResource(R.string.map_dist_m, meters.toInt())
                                    else stringResource(R.string.map_dist_km, meters / 1000f),
                                    fontSize = 11.sp, color = DtTheme.colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Coincide si el texto está en el nombre o en el tipo de cocina (sin importar mayúsculas). */
private fun Restaurant.matches(context: android.content.Context, text: String): Boolean {
    val cuisineText = cuisine ?: if (cuisineRes != 0) context.getString(cuisineRes) else ""
    return displayName(context).contains(text, ignoreCase = true) || cuisineText.contains(text, ignoreCase = true)
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
