package com.delitracker.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delitracker.app.R
import com.delitracker.app.components.DtRow
import com.delitracker.app.state.AppViewModel
import com.delitracker.app.ui.theme.DtAccent2
import com.delitracker.app.ui.theme.DtDanger
import com.delitracker.app.ui.theme.DtTheme

// Hub de navegación hacia cuenta y ajustes
@Composable
fun PerfilScreen(
    vm: AppViewModel,
    onEditarPerfil: () -> Unit,
    onFavoritos: () -> Unit,
    onCompartir: () -> Unit,
    onAjustes: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(DtTheme.colors.accentSoft)
                .clickable(onClick = onEditarPerfil),
            contentAlignment = Alignment.Center
        ) {
            Text(initialsOf(vm.userName), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DtTheme.colors.accentSoftInk)
        }
        Spacer(Modifier.height(8.dp))
        Text(vm.userName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(vm.userEmail, fontSize = 12.sp, color = DtTheme.colors.textMuted, modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(12.dp))
        DtRow(onClick = onFavoritos) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.favoritos), fontSize = 13.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DtTheme.colors.textMuted)
        }
        DtRow(onClick = onCompartir) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.compartir_ubicaci_n), fontSize = 13.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DtTheme.colors.textMuted)
        }
        DtRow(onClick = onAjustes) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.ajustes_de_cuenta), fontSize = 13.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DtTheme.colors.textMuted)
        }
    }
}

private fun initialsOf(name: String): String =
    name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

// Historia #7 · Foto de perfil desde galería
@Composable
fun EditarPerfilScreen(vm: AppViewModel, onGuardar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DtTheme.colors.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(initialsOf(vm.userName), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DtTheme.colors.accentSoftInk)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.cambiar_foto_desde_galer_a), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = vm.userName,
            onValueChange = { vm.userName = it },
            label = { Text(stringResource(R.string.nombre)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = vm.userEmail,
            onValueChange = { vm.userEmail = it },
            label = { Text(stringResource(R.string.correo)) },
            modifier = Modifier.fillMaxWidth()
        )

        vm.authMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.weight(1f))
        Button(
            enabled = !vm.authBusy,
            onClick = {
                vm.updateProfile(name = vm.userName, email = vm.userEmail) { success, _ ->
                    if (success) onGuardar()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text(stringResource(R.string.guardar_cambios))
        }
    }
}

// Hub hacia privacidad, Premium y sensores
@Composable
fun AjustesScreen(
    onPremium: () -> Unit,
    onContexto: () -> Unit,
    onCerrarSesion: () -> Unit,
    onEliminarCuenta: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        DtRow(onClick = onPremium) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = DtAccent2, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.plan_premium), fontSize = 13.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DtTheme.colors.textMuted)
        }
        DtRow(onClick = onContexto) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.contexto_de_sensores), fontSize = 13.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DtTheme.colors.textMuted)
        }
        DtRow {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.notificaciones), fontSize = 13.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DtTheme.colors.textMuted)
        }
        DtRow(onClick = onCerrarSesion) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.cerrar_sesi_n), fontSize = 13.sp)
            }
        }
        DtRow(onClick = onEliminarCuenta) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = DtDanger, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.eliminar_mi_cuenta), fontSize = 13.sp, color = DtDanger)
            }
        }
    }
}

// Historia #18 · Suscribirse al plan Premium
@Composable
fun PremiumScreen(onSuscribirse: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DtTheme.colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(stringResource(R.string.gratis), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.mapa_y_filtros), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
                Text(stringResource(R.string.notificaciones_b_sicas), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, DtAccent2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Surface(shape = RoundedCornerShape(6.dp), color = DtTheme.colors.accent2Soft) {
                    Text(
                        stringResource(R.string.recomendado), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = DtTheme.colors.accent2SoftInk,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string._2_99), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.mes), fontSize = 11.sp, color = DtTheme.colors.textMuted)
                }
                Text(stringResource(R.string.sin_publicidad), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
                Text(stringResource(R.string.ar_avanzado), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
                Text(stringResource(R.string.seguimiento_social_ilimitado), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onSuscribirse, modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)) {
            Text(stringResource(R.string.suscribirme))
        }
    }
}

// Historias #11 y #12 · Acelerómetro y giroscopio
@Composable
fun ContextoScreen() {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DtTheme.colors.accentSoft,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.DirectionsWalk, contentDescription = null, tint = DtTheme.colors.accentSoftInk, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surface) {
                    Text(stringResource(R.string.caminando), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
        Text(
            stringResource(R.string.confianza_del_sensor_aceler_metro),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
        )
        LinearProgressIndicator(progress = 0.65f, modifier = Modifier
            .fillMaxWidth()
            .height(6.dp))

        Text(
            stringResource(R.string.orientaci_n_giroscopio),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
        )
        DtRow {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Text(stringResource(R.string.apuntando_al_noreste), fontSize = 13.sp)
            }
        }
        TextButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.no_es_correcto_corregir_manualmente))
        }
    }
}

// Historia #19 · Cerrar sesión y eliminar datos
@Composable
fun LogoutScreen(vm: AppViewModel, onEliminar: () -> Unit, onCancelar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))
        Icon(Icons.Filled.Delete, contentDescription = null, tint = DtDanger, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.eliminar_tu_cuenta), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.se_borrar_n_tus_datos_personales_del_backend_esta_acci_n_no_se_puede_deshacer),
            fontSize = 12.5.sp, color = DtTheme.colors.textMuted, textAlign = TextAlign.Center
        )
        vm.authMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !vm.authBusy,
            onClick = onEliminar,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DtDanger)
        ) {
            Text(stringResource(R.string.eliminar_cuenta_y_datos))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCancelar, modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)) {
            Text(stringResource(R.string.cancelar))
        }
    }
}
