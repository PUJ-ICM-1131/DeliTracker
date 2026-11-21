package com.delitracker.app.share

import android.content.Context
import com.delitracker.app.route.LatLon
import com.delitracker.app.state.FirestoreProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Compartir ubicación en tiempo real con Cloud Firestore (historia #8).
 *
 * Documento `shares/{uidDelDueño}`:
 *   ownerName, ownerEmail, active, expiresAtMs, updatedAtMs, lat, lon,
 *   allowed: lista de correos (en minúscula) que pueden verlo.
 * Las reglas (firestore.rules) dejan escribir solo al dueño y leer solo al dueño y a los correos de `allowed`.
 * Un amigo escucha con una consulta `allowed array-contains <mi correo>`, así se entera al instante.
 *
 * No está disponible con el emulador local de Firebase (solo se inicia el de Authentication).
 */
internal object LocationShareRepository {

    private const val COLLECTION = "shares"

    val isAvailable: Boolean get() = FirestoreProvider.isAvailable

    private fun firestore(context: Context): FirebaseFirestore? = FirestoreProvider.get(context)

    private fun uid(context: Context): String? = FirestoreProvider.currentUid(context)

    private fun lowercased(emails: List<String>) = emails.map { it.trim().lowercase() }.distinct()

    /** Empieza (o reinicia) la sesión de compartir. Lanza una excepción si no hay sesión o falla la red. */
    suspend fun start(
        context: Context, name: String, email: String,
        allowed: List<String>, expiresAtMs: Long, position: LatLon?
    ) {
        val db = firestore(context) ?: error("Firestore no disponible")
        val uid = uid(context) ?: error("Sin sesión")
        val data = hashMapOf<String, Any?>(
            "ownerName" to name,
            "ownerEmail" to email.trim().lowercase(),
            "active" to true,
            "expiresAtMs" to expiresAtMs,
            "updatedAtMs" to System.currentTimeMillis(),
            "lat" to position?.lat,
            "lon" to position?.lon,
            "allowed" to lowercased(allowed)
        )
        db.collection(COLLECTION).document(uid).set(data).await()
    }

    suspend fun publish(context: Context, position: LatLon) {
        val db = firestore(context) ?: return
        val uid = uid(context) ?: return
        db.collection(COLLECTION).document(uid).update(
            mapOf("lat" to position.lat, "lon" to position.lon, "updatedAtMs" to System.currentTimeMillis())
        ).await()
    }

    suspend fun updateAllowed(context: Context, allowed: List<String>) {
        val db = firestore(context) ?: return
        val uid = uid(context) ?: return
        db.collection(COLLECTION).document(uid).update("allowed", lowercased(allowed)).await()
    }

    /** Termina la sesión: el documento queda inactivo y sin coordenadas. */
    suspend fun stop(context: Context) {
        val db = firestore(context) ?: return
        val uid = uid(context) ?: return
        db.collection(COLLECTION).document(uid).set(
            mapOf("active" to false, "lat" to null, "lon" to null, "allowed" to emptyList<String>()),
            SetOptions.merge()
        ).await()
    }

    /**
     * Escucha, en tiempo real, a los amigos que comparten contigo. Devuelve una función para dejar de escuchar,
     * o null si no es posible (sin sesión, Firestore no disponible).
     */
    fun observeFriends(
        context: Context,
        myEmail: String,
        onChange: (List<FriendLocation>) -> Unit,
        onError: () -> Unit
    ): (() -> Unit)? {
        val db = firestore(context) ?: return null
        val me = uid(context) ?: return null
        val registration = db.collection(COLLECTION)
            .whereArrayContains("allowed", myEmail.trim().lowercase())
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    onError()
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    if (doc.id == me || doc.getBoolean("active") != true) return@mapNotNull null
                    val lat = doc.getDouble("lat") ?: return@mapNotNull null
                    val lon = doc.getDouble("lon") ?: return@mapNotNull null
                    FriendLocation(
                        uid = doc.id,
                        name = doc.getString("ownerName").orEmpty().ifBlank { doc.getString("ownerEmail").orEmpty() },
                        email = doc.getString("ownerEmail").orEmpty(),
                        position = LatLon(lat, lon),
                        updatedAtMs = doc.getLong("updatedAtMs") ?: 0L,
                        expiresAtMs = doc.getLong("expiresAtMs") ?: 0L
                    )
                }
                onChange(list)
            }
        return { registration.remove() }
    }
}
