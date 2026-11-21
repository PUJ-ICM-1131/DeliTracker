package com.delitracker.app.share

import com.delitracker.app.route.LatLon

/** Amigo con el que se comparte la ubicación. Se identifica por su correo (el de su cuenta de DeliTracker). */
data class Friend(val email: String, val name: String, val enabled: Boolean = true)

/** Ubicación que un amigo está compartiendo contigo ahora mismo. */
data class FriendLocation(
    val uid: String,
    val name: String,
    val email: String,
    val position: LatLon,
    val updatedAtMs: Long,
    val expiresAtMs: Long
)

/** Una ubicación sin actualizar durante este tiempo se considera vieja y se oculta. */
const val STALE_AFTER_MS = 2 * 60 * 1000L

/** Duración por defecto de una sesión de compartir (la pantalla dice "1 hora"). */
const val DEFAULT_SHARE_DURATION_MS = 60 * 60 * 1000L
