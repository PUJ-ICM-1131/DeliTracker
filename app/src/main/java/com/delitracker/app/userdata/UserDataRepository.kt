package com.delitracker.app.userdata

import android.content.Context
import com.delitracker.app.state.FirestoreProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/** Preferencias del onboarding y de los filtros. Los textos son los mismos que se ven en los chips. */
data class SavedPreferences(
    val cuisines: List<String>,
    val diet: List<String>,
    val price: String?,
    val rating: String?
)

/** Lo guardado de un usuario: null en cada campo = nunca se guardó (usuario nuevo). */
data class UserData(val favorites: List<String>?, val preferences: SavedPreferences?)

/**
 * Datos propios de cada usuario en Cloud Firestore, en el documento `users/{uid}`
 * (lo leen y escriben solo su dueño; ver firestore.rules):
 *   favorites: nombres de los restaurantes favoritos (historia #13)
 *   cuisines, diet, price, rating: preferencias del onboarding y de los filtros (historia #14)
 *   reviewedRestaurants: ids de los restaurantes donde dejó reseña (para poder borrarlas, historia #19)
 *
 * No está disponible con el emulador local de Firebase (solo se inicia el de Authentication).
 */
internal object UserDataRepository {

    private const val USERS = "users"
    private const val SHARES = "shares"
    private const val RESTAURANTS = "restaurants"
    private const val REVIEWS = "reviews"

    suspend fun load(context: Context): UserData? {
        val db = FirestoreProvider.get(context) ?: return null
        val uid = FirestoreProvider.currentUid(context) ?: return null
        val doc = db.collection(USERS).document(uid).get().await()
        if (!doc.exists()) return UserData(null, null)
        fun strings(field: String): List<String>? =
            if (doc.contains(field)) (doc.get(field) as? List<*>)?.filterIsInstance<String>() else null
        val cuisines = strings("cuisines")
        val preferences = if (cuisines == null) null else SavedPreferences(
            cuisines = cuisines,
            diet = strings("diet").orEmpty(),
            price = doc.getString("price"),
            rating = doc.getString("rating")
        )
        return UserData(strings("favorites"), preferences)
    }

    suspend fun saveFavorites(context: Context, favorites: List<String>) {
        val db = FirestoreProvider.get(context) ?: return
        val uid = FirestoreProvider.currentUid(context) ?: return
        db.collection(USERS).document(uid).set(mapOf("favorites" to favorites), SetOptions.merge()).await()
    }

    suspend fun savePreferences(context: Context, preferences: SavedPreferences) {
        val db = FirestoreProvider.get(context) ?: return
        val uid = FirestoreProvider.currentUid(context) ?: return
        val data = hashMapOf<String, Any?>(
            "cuisines" to preferences.cuisines,
            "diet" to preferences.diet,
            "price" to preferences.price,
            "rating" to preferences.rating
        )
        db.collection(USERS).document(uid).set(data, SetOptions.merge()).await()
    }

    /** Recuerda en qué restaurantes dejó reseña el usuario, para borrarlas si elimina su cuenta. */
    suspend fun addReviewedRestaurant(context: Context, restaurantId: String) {
        val db = FirestoreProvider.get(context) ?: return
        val uid = FirestoreProvider.currentUid(context) ?: return
        db.collection(USERS).document(uid)
            .set(mapOf("reviewedRestaurants" to FieldValue.arrayUnion(restaurantId)), SetOptions.merge()).await()
    }

    /**
     * Borra todo lo que el usuario dejó en Firestore: sus reseñas, su ubicación compartida y su documento
     * (favoritos y preferencias). Debe llamarse ANTES de eliminar la cuenta de Authentication, porque las
     * reglas piden sesión iniciada. [extraRestaurantIds] cubre reseñas hechas antes de que se llevara este registro.
     * Lanza una excepción si algo falla, para que no se elimine la cuenta dejando datos atrás.
     */
    suspend fun deleteAll(context: Context, extraRestaurantIds: Collection<String>) {
        val db = FirestoreProvider.get(context) ?: return
        val uid = FirestoreProvider.currentUid(context) ?: return
        val doc = db.collection(USERS).document(uid).get().await()
        val saved = (doc.get("reviewedRestaurants") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val ids = (saved + extraRestaurantIds).filter { it.isNotBlank() }.distinct()
        // Borrar un documento que no existe no da error; un lote admite hasta 500 operaciones
        ids.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { id ->
                batch.delete(db.collection(RESTAURANTS).document(id).collection(REVIEWS).document(uid))
            }
            batch.commit().await()
        }
        db.collection(SHARES).document(uid).delete().await()
        db.collection(USERS).document(uid).delete().await()
    }
}
