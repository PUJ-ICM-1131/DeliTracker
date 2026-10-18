package com.delitracker.app.state

import android.content.Context

/** solo en este dispositivo, qué foto de perfil (ruta interna) corresponde a cada cuenta. */
class LocalPhotoStore(context: Context) {
    private val prefs = context.getSharedPreferences("local_photos", Context.MODE_PRIVATE)
    private fun key(email: String) = "photo_" + email.trim().lowercase()

    fun get(email: String): String? = prefs.getString(key(email), null)
    fun set(email: String, path: String) { prefs.edit().putString(key(email), path).apply() }
    fun remove(email: String) { prefs.edit().remove(key(email)).apply() }

    /** Si el usuario cambia su correo, la foto lo sigue. */
    fun move(oldEmail: String, newEmail: String) {
        if (key(oldEmail) == key(newEmail)) return
        val path = get(oldEmail) ?: return
        prefs.edit().remove(key(oldEmail)).putString(key(newEmail), path).apply()
    }
}