package com.delitracker.app.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.delitracker.app.R
import com.delitracker.app.storage.ImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoPicker internal constructor(val showOptions: () -> Unit)

/**
 * Selector de foto reutilizable: muestra un diálogo "Tomar foto / Elegir de
 * la galería", pide el permiso de cámara si hace falta, y entrega la RUTA de
 * la imagen ya guardada en el almacenamiento interno vía onPhotoSaved.
 *
 * Manejo del permiso de cámara:
 *  - Si ya está concedido → abre la cámara directamente.
 *  - Si no → lanza el diálogo del sistema.
 *  - Si el usuario NIEGA:
 *      · Si aún se puede volver a pedir → diálogo con "Reintentar".
 *      · Si marcó "No volver a preguntar" → diálogo con "Abrir Ajustes".
 *  - Al volver de Ajustes, se re-verifica el permiso en onResume.
 */
@Composable
fun rememberPhotoPicker(prefix: String, onPhotoSaved: (String) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnSaved by rememberUpdatedState(onPhotoSaved)

    var showDialog by remember { mutableStateOf(false) }
    // rememberSaveable: sobrevive a una rotación mientras la cámara está abierta
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    // Diálogos de permiso denegado
    var showRetryDialog by remember { mutableStateOf(false) }      // "Reintentar"
    var showSettingsDialog by remember { mutableStateOf(false) }   // "Abrir Ajustes"

    val cameraDeniedMsg = stringResource(R.string.photo_camera_denied)
    val saveErrorMsg = stringResource(R.string.photo_save_error)

    // ---------------------------------------------------------------
    // Galería (NO requiere permiso: Photo Picker del sistema)
    // ---------------------------------------------------------------
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val file = withContext(Dispatchers.IO) {
                    ImageStorage.copyFromUri(context, uri, prefix)
                }
                if (file != null) currentOnSaved(file.absolutePath)
                else Toast.makeText(context, saveErrorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------------------------------------------------------
    // Cámara
    // ---------------------------------------------------------------
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingPath
        pendingPath = null
        if (success && path != null) currentOnSaved(path)
        else ImageStorage.delete(context, path) // limpia archivo vacío si se canceló
    }

    fun launchCamera() {
        val file = ImageStorage.newImageFile(context, prefix)
        pendingPath = file.absolutePath
        cameraLauncher.launch(ImageStorage.uriFor(context, file))
    }

    // ---------------------------------------------------------------
    // Permiso de cámara
    // ---------------------------------------------------------------
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            // ¿Todavía se puede volver a pedir?
            val activity = context.findActivity()
            val canAskAgain = activity
                ?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true

            if (canAskAgain) showRetryDialog = true      // → Reintentar
            else showSettingsDialog = true               // → Ir a Ajustes
        }
    }

    // Al volver de Ajustes, si el usuario ya concedió el permiso, abrimos la cámara.
    // (Android no da callback al volver de Ajustes, así que re-verificamos en onResume.)
    DisposableEffect(lifecycleOwner) {
        var wasSettingsDialogShown = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && wasSettingsDialogShown) {
                wasSettingsDialogShown = false
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) launchCamera()
            }
        }
        // Marcamos que el diálogo de Ajustes fue mostrado para saber que venimos de allí
        if (showSettingsDialog) wasSettingsDialogShown = true
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ---------------------------------------------------------------
    // Diálogo principal: Tomar foto / Galería
    // ---------------------------------------------------------------
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.photo_dialog_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        showDialog = false
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) launchCamera()
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Text(stringResource(R.string.photo_take)) }

                    TextButton(onClick = {
                        showDialog = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) { Text(stringResource(R.string.photo_gallery)) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    // ---------------------------------------------------------------
    // Diálogo: reintentar permiso (aún se puede pedir)
    // ---------------------------------------------------------------
    if (showRetryDialog) {
        AlertDialog(
            onDismissRequest = { showRetryDialog = false },
            title = { Text(stringResource(R.string.photo_camera_denied_title)) },
            text = { Text(stringResource(R.string.photo_camera_retry_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showRetryDialog = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) { Text(stringResource(R.string.photo_retry)) }
            },
            dismissButton = {
                TextButton(onClick = { showRetryDialog = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    // ---------------------------------------------------------------
    // Diálogo: abrir Ajustes (marcó "No volver a preguntar")
    // ---------------------------------------------------------------
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(stringResource(R.string.photo_camera_denied_title)) },
            text = { Text(stringResource(R.string.photo_camera_settings_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    openAppSettings(context)
                }) { Text(stringResource(R.string.photo_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    return remember { PhotoPicker { showDialog = true } }
}

// -------------------------------------------------------------------
// Helpers
// -------------------------------------------------------------------

/** Sube por los ContextWrapper hasta encontrar la Activity. */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** Abre la pantalla de Ajustes de la app (donde el usuario puede habilitar el permiso). */
private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
