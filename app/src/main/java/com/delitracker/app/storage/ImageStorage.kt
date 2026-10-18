package com.delitracker.app.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

/**
 * Almacenamiento interno privado de la app: todo se guarda en
 * filesDir/images. Ninguna otra app puede leer esos archivos, y no se
 * necesita ningún permiso de almacenamiento.
 */
object ImageStorage {

    private fun imagesDir(context: Context): File =
        File(context.filesDir, "images").apply { mkdirs() }

    /** Crea (sin escribir aún) un archivo nuevo con nombre único. */
    fun newImageFile(context: Context, prefix: String): File =
        File(imagesDir(context), "${prefix}_${System.currentTimeMillis()}.jpg")

    /** Uri "content://" que la app de cámara puede usar para escribir en ese archivo. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copia una imagen elegida de la galería al almacenamiento interno. Llamar fuera del hilo principal. */
    fun copyFromUri(context: Context, source: Uri, prefix: String): File? {
        val target = newImageFile(context, prefix)
        return try {
            val input = context.contentResolver.openInputStream(source) ?: return null
            input.use { ins -> target.outputStream().use { out -> ins.copyTo(out) } }
            target
        } catch (e: IOException) {
            target.delete()
            null
        }
    }

    /** Borra una imagen guardada (solo si está dentro de la carpeta de la app). */
    fun delete(context: Context, path: String?) {
        if (path == null) return
        val file = File(path)
        if (file.parentFile == imagesDir(context)) file.delete()
    }
}
