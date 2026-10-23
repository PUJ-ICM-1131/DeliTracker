package com.delitracker.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import com.delitracker.app.route.hasLocationPermission
import com.delitracker.app.share.Friend
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delitracker.app.R
import com.delitracker.app.components.DtAvatar
import com.delitracker.app.components.DtCaption
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
            DtRow(onClick = { vm.openFavorite(name); onAbrirDetalle() }) {
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
    val context = LocalContext.current
    var newEmail by remember { mutableStateOf("") }

    // Se escuchan los amigos mientras la pantalla está abierta y se refresca "hace X" cada 15 s
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

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasLocationPermission(context)) vm.startSharing() else vm.shareMessage = R.string.share_err_permiso
    }
    fun startWithPermission() {
        if (hasLocationPermission(context)) vm.startSharing()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Column(modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())) {
            if (vm.friends.isEmpty()) {
                Text(stringResource(R.string.share_aun_sin_amigos), fontSize = 12.sp, color = DtTheme.colors.textMuted)
            }
            vm.friends.forEach { friend ->
                FriendShareRow(
                    name = friend.name,
                    subtitle = friend.email,
                    initials = initialsFrom(friend.name),
                    checked = friend.enabled,
                    onToggle = { vm.setFriendEnabled(friend.email, !friend.enabled) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text(stringResource(R.string.share_correo_amigo)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { if (vm.addFriend(newEmail)) newEmail = "" }) {
                    Text(stringResource(R.string.share_agregar))
                }
            }
            TextButton(onClick = onInvitar) { Text(stringResource(R.string.invitar_m_s_amigos)) }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(Icons.Filled.AccessTime, contentDescription = null, tint = DtTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (vm.isSharing) stringResource(R.string.share_activo_hasta, formatClock(vm.shareExpiresAtMs))
                    else stringResource(R.string.compartiendo_por_1_hora),
                    fontSize = 11.5.sp, color = DtTheme.colors.textMuted
                )
            }
            vm.shareMessage?.let {
                Text(stringResource(it), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
            }

            DtCaption(stringResource(R.string.share_amigos_contigo))
            val sharingWithMe = vm.friendLocations
            if (sharingWithMe.isEmpty()) {
                Text(stringResource(R.string.share_nadie), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
            }
            sharingWithMe.forEach { friend ->
                DtRow {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DtAvatar(initialsFrom(friend.name))
                        Spacer(Modifier.width(10.dp))
                        Text(friend.name, fontSize = 13.sp)
                    }
                    Text(timeAgo(vm.nowMs - friend.updatedAtMs), fontSize = 11.sp, color = DtTheme.colors.textMuted)
                }
            }
        }

        if (vm.isSharing) {
            Button(
                onClick = { vm.stopSharing(); onDetener() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.detener))
            }
        } else {
            Button(
                onClick = { startWithPermission() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(stringResource(R.string.share_empezar))
            }
        }
    }
}

@Composable
private fun FriendShareRow(name: String, subtitle: String, initials: String, checked: Boolean, onToggle: () -> Unit) {
    DtRow {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            DtAvatar(initials)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(name, fontSize = 13.sp)
                Text(subtitle, fontSize = 10.5.sp, color = DtTheme.colors.textMuted)
            }
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

private fun initialsFrom(name: String): String =
    name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

private fun formatClock(ms: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))

@Composable
private fun timeAgo(diffMs: Long): String {
    val seconds = (diffMs / 1000).coerceAtLeast(0)
    return if (seconds < 60) stringResource(R.string.share_hace_seg, seconds.toInt())
    else stringResource(R.string.share_hace_min, (seconds / 60).toInt())
}

// Historia #9 · Invitar desde contactos del teléfono
// Se usa el selector de contactos de Android: no hace falta el permiso READ_CONTACTS, porque el sistema
// solo entrega el contacto que la persona elige.
@Composable
fun InvitarScreen(vm: AppViewModel, onContinuar: () -> Unit) {
    val context = LocalContext.current

    // 1) Elegir un contacto con teléfono y abrir el SMS con la invitación
    val smsPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val contact = readContact(context, uri, ContactsContract.CommonDataKinds.Phone.NUMBER)
        val number = contact?.first
        if (number.isNullOrBlank()) return@rememberLauncherForActivityResult
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(number))).apply {
            putExtra("sms_body", context.getString(R.string.share_sms_texto, vm.userEmail))
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.share_sin_app_sms, Toast.LENGTH_SHORT).show()
        }
    }

    // 2) Elegir un contacto con correo y agregarlo a la lista de amigos
    val emailPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val contact = readContact(context, uri, ContactsContract.CommonDataKinds.Email.ADDRESS)
        val email = contact?.first
        if (email.isNullOrBlank()) {
            Toast.makeText(context, R.string.share_contacto_sin_correo, Toast.LENGTH_SHORT).show()
        } else {
            vm.addFriend(email, contact?.second)
        }
    }

    fun launchPicker(launcher: androidx.activity.result.ActivityResultLauncher<Intent>, contentUri: Uri) {
        try {
            launcher.launch(Intent(Intent.ACTION_PICK, contentUri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.share_sin_app_contactos, Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Column(modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())) {
            Button(
                onClick = { launchPicker(smsPicker, ContactsContract.CommonDataKinds.Phone.CONTENT_URI) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.share_invitar_sms)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { launchPicker(emailPicker, ContactsContract.CommonDataKinds.Email.CONTENT_URI) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.share_agregar_contacto)) }
            vm.shareMessage?.let {
                Text(stringResource(it), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
            }

            DtCaption(stringResource(R.string.share_tus_amigos))
            if (vm.friends.isEmpty()) {
                Text(stringResource(R.string.share_aun_sin_amigos), fontSize = 11.5.sp, color = DtTheme.colors.textMuted)
            }
            vm.friends.forEach { friend ->
                DtRow {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        DtAvatar(initialsFrom(friend.name))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(friend.name, fontSize = 13.sp)
                            Text(friend.email, fontSize = 10.5.sp, color = DtTheme.colors.textMuted)
                        }
                    }
                    IconButton(onClick = { vm.removeFriend(friend.email) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.share_quitar), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        TextButton(onClick = onContinuar, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.continuar_sin_invitar))
        }
    }
}

/** (valor de [column], nombre) del contacto elegido, o null si no se pudo leer. */
private fun readContact(context: Context, uri: Uri, column: String): Pair<String, String>? =
    try {
        context.contentResolver.query(
            uri, arrayOf(column, ContactsContract.Contacts.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) Pair(cursor.getString(0).orEmpty(), cursor.getString(1).orEmpty()) else null
        }
    } catch (e: SecurityException) {
        null
    }
