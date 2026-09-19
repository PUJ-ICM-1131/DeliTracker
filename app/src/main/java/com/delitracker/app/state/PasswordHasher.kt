package com.delitracker.app.state

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Hashing simple de contraseñas con sal aleatoria (SHA-256).
 * Evita guardar la contraseña en texto plano en el DataStore.
 * (Para producción real se recomendaría algo como BCrypt/Argon2 o,
 * mejor aún, delegar la autenticación a un backend/Firebase Auth).
 */
object PasswordHasher {

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun hash(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashed = digest.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashed, Base64.NO_WRAP)
    }

    fun encodeSalt(salt: ByteArray): String = Base64.encodeToString(salt, Base64.NO_WRAP)

    fun decodeSalt(encoded: String): ByteArray = Base64.decode(encoded, Base64.NO_WRAP)
}
