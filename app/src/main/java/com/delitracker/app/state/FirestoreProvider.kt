package com.delitracker.app.state

import android.content.Context
import com.delitracker.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Cloud Firestore del proyecto Firebase de la app (el mismo de Authentication).
 * No está disponible con el emulador local de Firebase: solo se inicia el de Authentication.
 */
internal object FirestoreProvider {

    val isAvailable: Boolean get() = !(BuildConfig.DEBUG && BuildConfig.FIREBASE_AUTH_EMULATOR)

    fun get(context: Context): FirebaseFirestore? {
        if (!isAvailable) return null
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(context)
            ?: return null
        return FirebaseFirestore.getInstance(app)
    }

    /** uid de la sesión actual, o null si no hay sesión o Firebase no está configurado. */
    fun currentUid(context: Context): String? =
        try { FirebaseAuthProvider.get(context).currentUser?.uid } catch (e: Exception) { null }
}
