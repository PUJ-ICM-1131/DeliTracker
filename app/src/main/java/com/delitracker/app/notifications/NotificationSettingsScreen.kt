package com.delitracker.app.notifications

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.delitracker.app.R

@Composable
fun NotificationSettingsScreen(account: String) {
    val context = LocalContext.current
    val notifications = remember { DeliNotifications(context.applicationContext) }
    var nearby by remember(account) { mutableStateOf(notifications.enabled(account, DeliNotifications.NEARBY)) }
    var social by remember(account) { mutableStateOf(notifications.enabled(account, DeliNotifications.SOCIAL)) }
    var systemEnabled by remember { mutableStateOf(notifications.systemEnabled()) }
    var channelsEnabled by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<Int?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        systemEnabled = notifications.systemEnabled()
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemEnabled = notifications.systemEnabled()
                channelsEnabled = notifications.systemEnabled(DeliNotifications.NEARBY) && notifications.systemEnabled(DeliNotifications.SOCIAL)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.notif_explanation))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.notif_nearby), Modifier.weight(1f))
            Switch(nearby, onCheckedChange = {
                nearby = it; notifications.setEnabled(account, DeliNotifications.NEARBY, it)
                if (it && !systemEnabled) requestPermission()
            })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.notif_social), Modifier.weight(1f))
            Switch(social, onCheckedChange = {
                social = it; notifications.setEnabled(account, DeliNotifications.SOCIAL, it)
                if (it && !systemEnabled) requestPermission()
            })
        }
        Text(stringResource(if (systemEnabled) R.string.notif_allowed else R.string.notif_blocked))
        if (systemEnabled && !channelsEnabled) Text(stringResource(R.string.notif_channel_blocked))
        if (!systemEnabled && Build.VERSION.SDK_INT >= 33) {
            Button(onClick = { requestPermission() }) { Text(stringResource(R.string.notif_permission)) }
        }
        OutlinedButton(onClick = {
            val intent = if (Build.VERSION.SDK_INT >= 26) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + context.packageName))
            context.startActivity(intent)
        }) { Text(stringResource(R.string.notif_system_settings)) }
        Button(onClick = {
            message = if (notifications.test(account)) R.string.notif_sent else R.string.notif_not_sent
        }, enabled = nearby && systemEnabled) { Text(stringResource(R.string.notif_test)) }
        message?.let { Text(stringResource(it)) }
        HorizontalDivider()
        Text(stringResource(R.string.notif_scope), style = MaterialTheme.typography.bodySmall)
    }
}
