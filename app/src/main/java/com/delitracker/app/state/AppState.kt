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

/**
 * Estado global de la app, equivalente al objeto `state` del prototipo JS.
 * Se inyecta una sola instancia en el NavHost para que sobreviva a la navegación
 * entre pantallas, igual que el `state` global del prototipo.
 *
 * Es un AndroidViewModel (en vez de un ViewModel simple) porque necesita
 * Context para leer/escribir el perfil y las credenciales guardadas en el
 * dispositivo (DataStore), y también para poder resolver textos de
 * strings.xml con application.getString(...) — aquí no se puede usar
 * stringResource() porque esta clase no es @Composable.
 */
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

    /** True si ya existe una cuenta creada en este dispositivo (registro completado). */
    var hasAccount by mutableStateOf(false)
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    var isReady by mutableStateOf(false)
        private set
    init {
        viewModelScope.launch {
            profileRepository.profileFlow.collect { stored ->
                userName = stored.name
                userEmail = stored.email
                hasAccount = stored.hasAccount
                isLoggedIn = stored.isLoggedIn
                isReady = true
            }
        }
    }

    /**
     * Historia #1 · Registro: guarda nombre, correo y contraseña (hasheada) en
     * el dispositivo. onResult(exito, idDeRecursoDeError).
     */
    fun register(name: String, email: String, password: String, onResult: (Boolean, Int?) -> Unit) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        when {
            trimmedName.isBlank() -> onResult(false, R.string.error_name_empty)
            trimmedEmail.isBlank() -> onResult(false, R.string.error_email_empty)
            password.isBlank() -> onResult(false, R.string.error_password_empty)
            password.length < 4 -> onResult(false, R.string.error_password_short)
            else -> viewModelScope.launch {
                profileRepository.registerAccount(trimmedName, trimmedEmail, password)
                profileRepository.setLoggedIn(true)
                userName = trimmedName
                userEmail = trimmedEmail
                hasAccount = true
                isLoggedIn = true
                onResult(true, null)
            }
        }
    }

    /**
     * Historia #2 · Login: valida el correo y la contraseña ingresados contra
     * los guardados al registrarse. onResult(exito, idDeRecursoDeError).
     */
    fun login(email: String, password: String, onResult: (Boolean, Int?) -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            onResult(false, R.string.ingresa_correo_y_contrase_a)
            return
        }
        viewModelScope.launch {
            when (profileRepository.verifyCredentials(trimmedEmail, password)) {
                LoginResult.SUCCESS -> {
                    profileRepository.setLoggedIn(true)
                    isLoggedIn = true
                    onResult(true, null)
                }
                LoginResult.WRONG_CREDENTIALS -> onResult(false, R.string.correo_o_contrase_a_incorrectos)
                LoginResult.NO_ACCOUNT -> onResult(false, R.string.no_hay_una_cuenta_registrada)
            }
        }
    }

    /** Editar perfil: actualiza nombre/correo (no toca la contraseña). */
    fun updateProfile(name: String, email: String) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        userName = trimmedName
        userEmail = trimmedEmail
        viewModelScope.launch {
            profileRepository.updateProfile(trimmedName, trimmedEmail)
        }
    }

    /** Al cerrar sesión o eliminar cuenta. */
    fun logout() {
        viewModelScope.launch {
            profileRepository.setLoggedIn(false)
            isLoggedIn = false
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
