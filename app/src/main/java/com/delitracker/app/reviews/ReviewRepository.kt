package com.delitracker.app.reviews

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.delitracker.app.state.FirestoreProvider
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Reseñas de restaurantes en Cloud Firestore.
 *
 * Documento `restaurants/{idDelRestaurante}/reviews/{uidDelAutor}`: una reseña por persona y lugar
 * (volver a publicar la reemplaza). Campos: uid, userName, stars, text, photo (JPEG en Base64, opcional), createdAtMs.
 *
 * La foto NO va a Firebase Storage (desde 2024 exige el plan de pago): se reduce a ~800 px y calidad 70 y se guarda
 * como texto Base64 dentro del documento (un documento admite 1 MiB). Cuesta unos 60-150 KB por foto.
 */
internal object ReviewRepository {

    private const val RESTAURANTS = "restaurants"
    private const val REVIEWS = "reviews"
    const val MAX_PHOTO_CHARS = 400_000
    private const val MAX_SIDE_PX = 800
    private const val MAX_TEXT = 1000
    private const val LATEST = 30L

    val isAvailable: Boolean get() = FirestoreProvider.isAvailable

    /** Las 30 reseñas más recientes del restaurante. Lanza una excepción si falla la red o no hay permiso. */
    suspend fun load(context: Context, restaurantId: String): List<Review> {
        val db = FirestoreProvider.get(context) ?: return emptyList()
        val snapshot = db.collection(RESTAURANTS).document(restaurantId).collection(REVIEWS)
            .orderBy("createdAtMs", Query.Direction.DESCENDING)
            .limit(LATEST)
            .get().await()
        return snapshot.documents.mapNotNull { doc ->
            val stars = doc.getLong("stars")?.toInt() ?: return@mapNotNull null
            Review(
                uid = doc.id,
                userName = doc.getString("userName").orEmpty().ifBlank { "—" },
                stars = stars.coerceIn(1, 5),
                text = doc.getString("text").orEmpty(),
                photo = doc.getString("photo")?.takeIf { it.isNotEmpty() }?.let {
                    try { Base64.decode(it, Base64.DEFAULT) } catch (e: IllegalArgumentException) { null }
                },
                createdAtMs = doc.getLong("createdAtMs") ?: 0L
            )
        }
    }

    /** Publica (o reemplaza) la reseña del usuario actual. [photoPath] es el archivo local de la foto, si hay. */
    suspend fun publish(
        context: Context, restaurantId: String, userName: String,
        stars: Int, text: String, photoPath: String?
    ) {
        val db = FirestoreProvider.get(context) ?: error("Firestore no disponible")
        val uid = FirestoreProvider.currentUid(context) ?: error("Sin sesión")
        val photo = photoPath?.let { withContext(Dispatchers.IO) { compressToBase64(it) } }
        val data = hashMapOf<String, Any>(
            "uid" to uid,
            "userName" to userName,
            "stars" to stars.coerceIn(1, 5),
            "text" to text.trim().take(MAX_TEXT),
            "createdAtMs" to System.currentTimeMillis()
        )
        if (photo != null) data["photo"] = photo
        db.collection(RESTAURANTS).document(restaurantId).collection(REVIEWS).document(uid).set(data).await()
    }

    /** JPEG de máx. 800 px y calidad 70 en Base64; null si el archivo no existe o no se puede leer. */
    private fun compressToBase64(path: String): String? {
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val scale = MAX_SIDE_PX.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        } else bitmap
        var quality = 70
        var encoded: String
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            quality -= 15
        } while (encoded.length > MAX_PHOTO_CHARS && quality >= 25)
        if (scaled !== bitmap) bitmap.recycle()
        return encoded.takeIf { it.length <= MAX_PHOTO_CHARS }
    }
}
