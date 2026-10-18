package com.delitracker.app.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.delitracker.app.R
import kotlinx.coroutines.launch

/** Estado de interfaz y sesión validada por el backend del proyecto. */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val profileRepository = UserProfileRepository(application)

    // Filtros / onboarding (las claves salen de strings.xml para no duplicar
    // el texto que ya se usa al mostrar los chips en las pantallas)
    val cuisine = mutableStateMapOf(
        application.getString(R.string.italiana) to true,
        application.getString(R.string.mexicana) to false,
        application.getString(R.string.vegana) to true,
        application.getString(R.string.sushi) to false,
        application.getString(R.string.sin_gluten) to false
    )
    var price by mutableStateOf(application.getString(R.string.precio_medio))
    var rating by mutableStateOf(application.getString(R.string._4))
    val diet = mutableStateMapOf(
        application.getString(R.string.sin_gluten) to true,
        application.getString(R.string.vegetariana) to false
    )

    // Detalle de restaurante
    var favorited by mutableStateOf(true)

    // Compartir ubicación
    var shareMc by mutableStateOf(true)
    var shareJd by mutableStateOf(false)
    var shareLp by mutableStateOf(false)

    // Reseña
    var stars by mutableStateOf(4)
    var reviewText by mutableStateOf(application.getString(R.string.review_placeholder_default))

    // Ruta: "walk" | "auto" | "bus" — clave interna de estado, no es texto
    // visible al usuario (el texto que sí se ve sale de strings.xml en la
    // propia pantalla), por eso NO se resuelve contra strings.xml aquí.
    var travelMode by mutableStateOf("walk")

    // Favoritos
    val favs = mutableStateListOf(
        application.getString(R.string.trattoria_sole),
        application.getString(R.string.verde_bowl),
        application.getString(R.string.sushi_ten_),
        application.getString(R.string.cafe_andino)
    )

    // Alerta simulada en el mapa (historia de notificaciones geolocalizadas)
    var showAlertToast by mutableStateOf(false)

    // Perfil (se sobrescribe con lo guardado en el dispositivo, si existe)
    var userName by mutableStateOf("")
    var userEmail by mutableStateOf("")

    var hasAccount by mutableStateOf(false)
        private set
    var isLoggedIn by mutableStateOf(false)
        private set
    var isReady by mutableStateOf(false)
        private set
    var authBusy by mutableStateOf(false)
        private set
    var authMessage by mutableStateOf<Int?>(null)
        private set

    private fun applyProfile(profile: RemoteProfile) {
        userName = profile.name
        userEmail = profile.email
        hasAccount = true
        isLoggedIn = true
    }

    init {
        viewModelScope.launch {
            try { profileRepository.restore()?.let { applyProfile(it) } }
            catch (e: java.io.IOException) { authMessage = R.string.auth_network_error }
            catch (e: AuthFailure) { authMessage = errorFor(e) }
            catch (e: org.json.JSONException) { authMessage = R.string.auth_server_error }
            finally { isReady = true }
        }
    }

    private fun errorFor(e: Exception): Int = when (e) {
        is AuthFailure -> when (e.code) {
            "email_exists" -> R.string.auth_email_exists
            "invalid_credentials" -> R.string.correo_o_contrase_a_incorrectos
            "unauthorized" -> R.string.auth_session_expired
            "invalid_input" -> R.string.auth_invalid_input
            "rate_limited" -> R.string.auth_rate_limited
            else -> R.string.auth_server_error
        }
        is java.io.IOException -> R.string.auth_network_error
        else -> R.string.auth_server_error
    }

    private fun execute(onResult: (Boolean, Int?) -> Unit, action: suspend () -> Unit) {
        if (authBusy) return
        authBusy = true
        authMessage = null
        viewModelScope.launch {
            try { action(); onResult(true, null) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                if (e is AuthFailure && e.status == 401) isLoggedIn = false
                authMessage = errorFor(e)
                onResult(false, authMessage)
            } finally { authBusy = false }
        }
    }

    fun register(name: String, email: String, password: String, onResult: (Boolean, Int?) -> Unit) {
        when {
            name.trim().isEmpty() -> onResult(false, R.string.error_name_empty)
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> onResult(false, R.string.auth_invalid_email)
            password.length < 8 -> onResult(false, R.string.auth_password_short)
            else -> execute(onResult) { applyProfile(profileRepository.authenticate(name.trim(), email.trim(), password)) }
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, Int?) -> Unit) {
        if (email.isBlank() || password.isBlank()) { onResult(false, R.string.ingresa_correo_y_contrase_a); return }
        execute(onResult) { applyProfile(profileRepository.authenticate(null, email.trim(), password)) }
    }

    fun updateProfile(name: String, email: String, onResult: (Boolean, Int?) -> Unit) {
        execute(onResult) { applyProfile(profileRepository.updateProfile(name.trim(), email.trim())) }
    }

    fun deleteAccount(onResult: (Boolean, Int?) -> Unit) {
        execute(onResult) {
            profileRepository.deleteAccount()
            isLoggedIn = false
            hasAccount = false
            userName = ""
            userEmail = ""
        }
    }

    fun logout(onDone: () -> Unit) {
        if (authBusy) return
        authBusy = true
        viewModelScope.launch {
            try { profileRepository.logout() }
            catch (e: java.io.IOException) { authMessage = R.string.auth_logout_offline }
            catch (e: AuthFailure) { authMessage = R.string.auth_logout_offline }
            catch (e: org.json.JSONException) { authMessage = R.string.auth_logout_offline }
            finally {
                isLoggedIn = false
                userName = ""
                userEmail = ""
                authBusy = false
                onDone()
            }
        }
    }

    fun toggleCuisine(key: String) {
        cuisine[key] = !(cuisine[key] ?: false)
    }

    fun toggleDiet(key: String) {
        diet[key] = !(diet[key] ?: false)
    }

    fun toggleFavorited() {
        favorited = !favorited
    }

    fun triggerAlertToast() {
        showAlertToast = true
    }

    fun dismissAlertToast() {
        showAlertToast = false
    }
}
