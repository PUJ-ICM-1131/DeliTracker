package com.delitracker.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delitracker.app.R
import com.delitracker.app.nav.Routes
import com.delitracker.app.nav.TopBarStyle
import com.delitracker.app.ui.theme.DtTheme
import coil.compose.AsyncImage
import java.io.File

/** Equivalente a topbar(title, opts) del prototipo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DtTopBar(title: String, style: TopBarStyle, onBack: () -> Unit) {
    if (style == TopBarStyle.NONE) return
    TopAppBar(
        title = { Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = if (style == TopBarStyle.CLOSE) Icons.Filled.Close else Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.volver)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/** Equivalente a tabbar() del prototipo: Inicio (mapa), Favoritos, Perfil. */
@Composable
fun DtBottomBar(currentRoute: String, onTabSelected: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.MAPA,
            onClick = { onTabSelected(Routes.MAPA) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.inicio)) }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.FAVORITOS,
            onClick = { onTabSelected(Routes.FAVORITOS) },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            label = { Text(stringResource(R.string.favoritos)) }
        )
        NavigationBarItem(
            selected = currentRoute == Routes.PERFIL,
            onClick = { onTabSelected(Routes.PERFIL) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text(stringResource(R.string.perfil)) }
        )
    }
}

/** Equivalente a .chip del prototipo. */
@Composable
fun DtChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = DtTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) colors.accentSoft else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else colors.border,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (selected) colors.accentSoftInk else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Fila de chips con salto de línea automático (equivalente a .chiprow / chipRow()). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DtChipRow(
    options: List<String>,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { opt ->
            DtChip(label = opt, selected = isSelected(opt), onClick = { onToggle(opt) })
        }
    }
}

/** Fila de un único valor seleccionado (equivalente a pillRow() para price/rating). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DtPillRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { opt ->
            DtChip(label = opt, selected = opt == selected, onClick = { onSelect(opt) })
        }
    }
}

/** Avatar circular con iniciales, equivalente a .avatar / .avatarbig. */
@Composable
fun DtAvatar(initials: String, size: Dp = 30.dp) {
    val colors = DtTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.accentSoft),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, fontSize = (size.value / 2.6).sp, fontWeight = FontWeight.Bold, color = colors.accentSoftInk)
    }
}

/** Fila de lista genérica, equivalente a .row. */
@Composable
fun DtRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            content()
        }
        Divider(color = DtTheme.colors.border, thickness = 0.6.dp)
    }
}

/** Campo de sección, equivalente a .caption. */
@Composable
fun DtCaption(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

/** Avatar grande: muestra la foto guardada en almacenamiento interno o, si no hay, las iniciales. */
@Composable
fun ProfileAvatar(photoPath: String?, initials: String, size: Dp, modifier: Modifier = Modifier) {
    val colors = DtTheme.colors
    val hasPhoto = remember(photoPath) { photoPath != null && File(photoPath).exists() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.accentSoft),
        contentAlignment = Alignment.Center
    ) {
        if (hasPhoto) {
            AsyncImage(
                model = File(photoPath!!),
                contentDescription = stringResource(R.string.profile_photo_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(initials, fontSize = (size.value / 3.6).sp, fontWeight = FontWeight.Bold, color = colors.accentSoftInk)
        }
    }
}
