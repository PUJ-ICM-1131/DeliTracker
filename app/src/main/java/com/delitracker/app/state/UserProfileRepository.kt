package com.delitracker.app.state

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persistencia local del perfil y credenciales de la persona que se registra
 * en DeliTracker. Usa Jetpack DataStore (reemplazo moderno de SharedPreferences):
 * guarda datos clave-valor en un archivo del propio dispositivo, sin backend.
 *
 * La autenticación es "real" a nivel local: al iniciar sesión se compara el
 * correo y el hash de la contraseña ingresados contra lo guardado en el
 * registro. No hay servidor: esto valida contra la única cuenta creada en
 * este dispositivo.
 */
private val Context.userDataStore by preferencesDataStore(name = "delitracker_user_profile")

private object ProfileKeys {
    val NAME = stringPreferencesKey("profile_name")
    val EMAIL = stringPreferencesKey("profile_email")
    val PASSWORD_HASH = stringPreferencesKey("profile_password_hash")
    val PASSWORD_SALT = stringPreferencesKey("profile_password_salt")
    val HAS_ACCOUNT = booleanPreferencesKey("profile_has_account")

    val IS_LOGGED_IN = booleanPreferencesKey("profile_is_logged_in")
}

data class StoredProfile(
    val name: String,
    val email: String,
    val hasAccount: Boolean,
    val isLoggedIn: Boolean = false
)

enum class LoginResult { SUCCESS, WRONG_CREDENTIALS, NO_ACCOUNT }

class UserProfileRepository(private val context: Context) {

    /** Emite el perfil guardado cada vez que cambia. */
    val profileFlow: Flow<StoredProfile> = context.userDataStore.data.map { prefs ->
        StoredProfile(
            name = prefs[ProfileKeys.NAME] ?: "",
            email = prefs[ProfileKeys.EMAIL] ?: "",
            hasAccount = prefs[ProfileKeys.HAS_ACCOUNT] ?: false,
            isLoggedIn = prefs[ProfileKeys.IS_LOGGED_IN] ?: false   // NUEVO
        )
    }

    /** Se llama al registrarse: guarda nombre, correo y la contraseña (hasheada con sal). */
    suspend fun registerAccount(name: String, email: String, password: String) {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(password, salt)
        context.userDataStore.edit { prefs ->
            prefs[ProfileKeys.NAME] = name
            prefs[ProfileKeys.EMAIL] = email
            prefs[ProfileKeys.PASSWORD_HASH] = hash
            prefs[ProfileKeys.PASSWORD_SALT] = PasswordHasher.encodeSalt(salt)
            prefs[ProfileKeys.HAS_ACCOUNT] = true
        }
    }

    /** Se llama al editar perfil: solo actualiza nombre/correo, no toca la contraseña. */
    suspend fun updateProfile(name: String, email: String) {
        context.userDataStore.edit { prefs ->
            prefs[ProfileKeys.NAME] = name
            prefs[ProfileKeys.EMAIL] = email
        }
    }
    /** Marca la sesión como activa (tras login/registro). */
    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[ProfileKeys.IS_LOGGED_IN] = loggedIn
        }
    }


    /**
     * Verifica que el correo y la contraseña ingresados en Login coincidan
     * exactamente con los guardados en el registro. Devuelve un resultado
     * explicando por qué falló, para poder mostrar el mensaje adecuado.
     */
    suspend fun verifyCredentials(email: String, password: String): LoginResult {
        val prefs = context.userDataStore.data.first()
        val hasAccount = prefs[ProfileKeys.HAS_ACCOUNT] ?: false
        if (!hasAccount) return LoginResult.NO_ACCOUNT

        val storedEmail = prefs[ProfileKeys.EMAIL] ?: return LoginResult.NO_ACCOUNT
        val storedHash = prefs[ProfileKeys.PASSWORD_HASH] ?: return LoginResult.NO_ACCOUNT
        val storedSalt = prefs[ProfileKeys.PASSWORD_SALT] ?: return LoginResult.NO_ACCOUNT

        if (!storedEmail.equals(email.trim(), ignoreCase = true)) return LoginResult.WRONG_CREDENTIALS

        val enteredHash = PasswordHasher.hash(password, PasswordHasher.decodeSalt(storedSalt))
        return if (enteredHash == storedHash) LoginResult.SUCCESS else LoginResult.WRONG_CREDENTIALS
    }
}
