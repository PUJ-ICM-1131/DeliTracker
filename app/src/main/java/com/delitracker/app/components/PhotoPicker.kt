package com.delitracker.app.components

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
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
 */
@Composable
fun rememberPhotoPicker(prefix: String, onPhotoSaved: (String) -> Unit): PhotoPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnSaved by rememberUpdatedState(onPhotoSaved)
    var showDialog by remember { mutableStateOf(false) }
    // rememberSaveable: sobrevive a una rotación mientras la cámara está abierta
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraDeniedMsg = stringResource(R.string.photo_camera_denied)
    val saveErrorMsg = stringResource(R.string.photo_save_error)

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val file = withContext(Dispatchers.IO) { ImageStorage.copyFromUri(context, uri, prefix) }
                if (file != null) currentOnSaved(file.absolutePath)
                else Toast.makeText(context, saveErrorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = pendingPath
        pendingPath = null
        if (success && path != null) currentOnSaved(path)
        else ImageStorage.delete(context, path)
    }

    fun launchCamera() {
        val file = ImageStorage.newImageFile(context, prefix)
        pendingPath = file.absolutePath
        cameraLauncher.launch(ImageStorage.uriFor(context, file))
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(context, cameraDeniedMsg, Toast.LENGTH_SHORT).show()
    }

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
                        if (granted) launchCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
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
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancelar)) }
            }
        )
    }

    return remember { PhotoPicker { showDialog = true } }
}
