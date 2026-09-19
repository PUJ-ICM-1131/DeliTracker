package com.delitracker.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delitracker.app.R
import com.delitracker.app.components.DtAvatar
import com.delitracker.app.components.DtRow
import com.delitracker.app.state.AppViewModel
import com.delitracker.app.ui.theme.DtDanger
import com.delitracker.app.ui.theme.DtTheme

// Historia #13 · Guardar restaurantes favoritos
@Composable
fun FavoritosScreen(vm: AppViewModel, onAbrirDetalle: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(stringResource(R.string.favoritos), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        vm.favs.forEach { name ->
            DtRow(onClick = onAbrirDetalle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DtTheme.colors.surface2)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = DtDanger, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// Historia #8 · Ubicación en tiempo real con amigos
@Composable
fun CompartirScreen(vm: AppViewModel, onInvitar: () -> Unit, onDetener: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        FriendShareRow(name = stringResource(R.string.mar_a_camila), initials = stringResource(R.string.mc), checked = vm.shareMc, onToggle = { vm.shareMc = !vm.shareMc })
        FriendShareRow(name = stringResource(R.string.juan_david), initials = stringResource(R.string.jd), checked = vm.shareJd, onToggle = { vm.shareJd = !vm.shareJd })
        FriendShareRow(name = stringResource(R.string.laura_p), initials = stringResource(R.string.lp), checked = vm.shareLp, onToggle = { vm.shareLp = !vm.shareLp })

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Icon(Icons.Filled.AccessTime, contentDescription = null, tint = DtTheme.colors.textMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.compartiendo_por_1_hora), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
        }
        TextButton(onClick = onInvitar) { Text(stringResource(R.string.invitar_m_s_amigos)) }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onDetener,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(R.string.detener))
        }
    }
}

@Composable
private fun FriendShareRow(name: String, initials: String, checked: Boolean, onToggle: () -> Unit) {
    DtRow {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DtAvatar(initials)
            Spacer(Modifier.width(10.dp))
            Text(name, fontSize = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

// Historia #9 · Invitar desde contactos del teléfono
@Composable
fun InvitarScreen(onContinuar: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        DtRow {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DtAvatar(stringResource(R.string.mc))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.mar_a_camila), fontSize = 13.sp)
            }
            Text(stringResource(R.string.ya_usa_la_app), fontSize = 11.sp, color = DtTheme.colors.textMuted)
        }
        DtRow {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DtAvatar(stringResource(R.string.jd))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.juan_david), fontSize = 13.sp)
            }
            OutlinedButton(onClick = {}, modifier = Modifier.height(30.dp)) { Text(stringResource(R.string.invitar), fontSize = 11.sp) }
        }
        DtRow {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DtAvatar(stringResource(R.string.lp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.laura_p), fontSize = 13.sp)
            }
            OutlinedButton(onClick = {}, modifier = Modifier.height(30.dp)) { Text(stringResource(R.string.invitar), fontSize = 11.sp) }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onContinuar, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.continuar_sin_invitar))
        }
    }
}
