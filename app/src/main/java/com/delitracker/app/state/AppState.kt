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
import com.delitracker.app.data.Restaurant
import com.delitracker.app.data.RestaurantCatalog
import com.delitracker.app.data.displayName
import com.delitracker.app.route.LatLon
import com.delitracker.app.reviews.Review
import com.delitracker.app.reviews.ReviewRepository
import com.delitracker.app.share.DEFAULT_SHARE_DURATION_MS
import com.delitracker.app.share.STALE_AFTER_MS
import com.delitracker.app.share.Friend
import com.delitracker.app.share.FriendLocation
import com.delitracker.app.share.FriendStore
import com.delitracker.app.share.LocationShareRepository
import com.delitracker.app.share.LocationSharer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.delitracker.app.storage.ImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Estado de interfaz y sesión validada por Firebase Authentication. */
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

    // Restaurante elegido en el mapa: lo usan Detalle, Ruta y Reseña (null = el primero del catálogo)
    var selectedRestaurant by mutableStateOf<Restaurant?>(null)
        private set
    // Restaurantes ya vistos, por nombre: permite abrir su detalle desde Favoritos
    private val seenRestaurants = mutableMapOf<String, Restaurant>()
    val currentRestaurant: Restaurant get() = selectedRestaurant ?: RestaurantCatalog.all.first()

    // Última ubicación conocida (la publica la pantalla del mapa); sirve para mostrar la distancia en Detalle
    var userLocation by mutableStateOf<LatLon?>(null)

    // Compartir ubicación en tiempo real (historias 8 y 9) — ver share/ y wiki/Compartir-Ubicacion.md
    private val friendStore = FriendStore(application)
    private val sharer = LocationSharer(application)
    val friends = mutableStateListOf<Friend>()
    var isSharing by mutableStateOf(false)
        private set
    var shareExpiresAtMs by mutableStateOf(0L)
        private set
    /** Texto de error/aviso (R.string) para las pantallas de compartir. */
    var shareMessage by mutableStateOf<Int?>(null)
    private var rawFriendLocations by mutableStateOf<List<FriendLocation>>(emptyList())
    var nowMs by mutableStateOf(System.currentTimeMillis())
        private set
    private var expiryJob: Job? = null
    private var watchers = 0
    private var stopWatching: (() -> Unit)? = null
    private var lastPublishMs = 0L
    private var startingShare = false

    /** Amigos que comparten contigo y cuyo dato no está vencido ni viejo. */
    val friendLocations: List<FriendLocation>
        get() = rawFriendLocations.filter { it.expiresAtMs > nowMs && nowMs - it.updatedAtMs < STALE_AFTER_MS }

    // Reseña
    var stars by mutableStateOf(4)
    var reviewText by mutableStateOf("")

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
    private val photoStore = LocalPhotoStore(application)

    var userPhotoPath by mutableStateOf<String?>(null)
        private set
    var reviewPhotoPath by mutableStateOf<String?>(null)
        private set

    private suspend fun deletePhotoFile(path: String?) = withContext(Dispatchers.IO) {
        ImageStorage.delete(getApplication<Application>(), path)
    }
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
    userPhotoPath = photoStore.get(profile.email)
    friends.clear()
    friends.addAll(friendStore.load(profile.email))
    hasAccount = true
    isLoggedIn = true
    }

    init {
        viewModelScope.launch {
            try { profileRepository.restore()?.let { applyProfile(it) } }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { authMessage = errorFor(e) }
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
            "network_error" -> R.string.auth_network_error
            "firebase_not_configured" -> R.string.auth_firebase_not_configured
            "provider_disabled" -> R.string.auth_provider_disabled
            "recent_login_required" -> R.string.auth_recent_login_required
            "invalid_email" -> R.string.auth_invalid_email
            "weak_password" -> R.string.auth_password_policy
            "profile_incomplete" -> R.string.auth_profile_incomplete
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
    execute(onResult) {
        val oldEmail = userEmail
        val updated = profileRepository.updateProfile(name.trim(), email.trim())
        photoStore.move(oldEmail, updated.email)
        applyProfile(updated)
        if (updated.emailVerificationPending) authMessage = R.string.auth_email_verification_sent
    }
}

    fun deleteAccount(onResult: (Boolean, Int?) -> Unit) {
    execute(onResult) {
        val email = userEmail
        stopSharingNow()
        clearShareState()
        profileRepository.deleteAccount()
        deletePhotoFile(photoStore.get(email))
        photoStore.remove(email)
        userPhotoPath = null
        reviewPhotoPath = null
        isLoggedIn = false
        hasAccount = false
        userName = ""
        userEmail = ""
    }
}

    fun resetPassword(email: String) {
        execute({ success, _ -> if (success) authMessage = R.string.auth_password_reset_sent }) {
            profileRepository.sendPasswordReset(email)
        }
    }

    fun logout(onDone: () -> Unit) {
        if (authBusy) return
        authBusy = true
        authMessage = null
        viewModelScope.launch {
            stopSharingNow()
            clearShareState()
            try { profileRepository.logout() }
            catch (e: java.io.IOException) { authMessage = R.string.auth_logout_offline }
            catch (e: AuthFailure) { authMessage = R.string.auth_logout_offline }
            catch (e: org.json.JSONException) { authMessage = R.string.auth_logout_offline }
            finally {
                isLoggedIn = false
                userName = ""
                userEmail = ""
                userPhotoPath = null
                reviewPhotoPath = null
                authBusy = false
                onDone()
            }
        }
    }
    fun setProfilePhoto(path: String) {
        val email = userEmail
        if (email.isBlank()) return
        val old = userPhotoPath
        photoStore.set(email, path)
        userPhotoPath = path
        if (old != null && old != path) viewModelScope.launch { deletePhotoFile(old) }
    }

    fun setReviewPhoto(path: String) {
        val old = reviewPhotoPath
        reviewPhotoPath = path
        if (old != null && old != path) viewModelScope.launch { deletePhotoFile(old) }
    }
    fun toggleCuisine(key: String) {
        cuisine[key] = !(cuisine[key] ?: false)
    }

    fun toggleDiet(key: String) {
        diet[key] = !(diet[key] ?: false)
    }

    fun selectRestaurant(restaurant: Restaurant) {
        seenRestaurants[restaurant.displayName(getApplication())] = restaurant
        selectedRestaurant = restaurant
    }

    fun selectCatalogRestaurant(id: String) {
        RestaurantCatalog.all.firstOrNull { it.id == id }?.let { selectRestaurant(it) }
    }

    /** Abre el detalle del favorito con ese nombre (si no se conoce, queda el último elegido). */
    fun openFavorite(name: String) {
        val app = getApplication<Application>()
        val found = seenRestaurants[name] ?: RestaurantCatalog.all.firstOrNull { it.displayName(app) == name }
        if (found != null) selectedRestaurant = found
    }

    // ---------------- Reseñas (Firestore) ----------------

    /** Reseñas del restaurante abierto en Detalle. */
    var reviews by mutableStateOf<List<Review>>(emptyList())
        private set
    var reviewsLoading by mutableStateOf(false)
        private set
    var reviewsError by mutableStateOf<Int?>(null)
        private set
    var reviewBusy by mutableStateOf(false)
        private set
    var reviewMessage by mutableStateOf<Int?>(null)
    private var reviewsJob: Job? = null

    fun loadReviews() {
        val restaurantId = currentRestaurant.id
        reviewsJob?.cancel()
        reviews = emptyList()
        reviewsError = null
        reviewsLoading = false
        if (!ReviewRepository.isAvailable) { reviewsError = R.string.review_err_no_disponible; return }
        if (!isLoggedIn) return
        reviewsLoading = true
        reviewsJob = viewModelScope.launch {
            try {
                reviews = ReviewRepository.load(getApplication(), restaurantId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                reviewsError = R.string.review_err_cargar
            } finally {
                reviewsLoading = false
            }
        }
    }

    /** Publica la reseña del restaurante elegido (una por persona: volver a publicar la reemplaza). */
    fun publishReview(onResult: (Boolean) -> Unit) {
        if (reviewBusy) return
        reviewMessage = null
        if (!ReviewRepository.isAvailable) { reviewMessage = R.string.review_err_no_disponible; onResult(false); return }
        if (!isLoggedIn) { reviewMessage = R.string.review_err_sin_sesion; onResult(false); return }
        val restaurant = currentRestaurant
        val rating = stars
        val text = reviewText
        val photo = reviewPhotoPath
        reviewBusy = true
        viewModelScope.launch {
            try {
                ReviewRepository.publish(getApplication(), restaurant.id, userName, rating, text, photo)
                deletePhotoFile(photo) // la foto ya está en Firebase
                reviewPhotoPath = null
                reviewText = ""
                stars = 4
                onResult(true)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                reviewMessage = R.string.review_err_publicar
                onResult(false)
            } finally {
                reviewBusy = false
            }
        }
    }

    // ---------------- Compartir ubicación ----------------

    private fun saveFriends() = friendStore.save(userEmail, friends.toList())

    private fun enabledEmails() = friends.filter { it.enabled }.map { it.email }

    /** Devuelve true si se agregó. Deja el motivo en [shareMessage] si no. */
    fun addFriend(email: String, name: String? = null): Boolean {
        val clean = email.trim().lowercase()
        shareMessage = null
        when {
            !android.util.Patterns.EMAIL_ADDRESS.matcher(clean).matches() -> { shareMessage = R.string.share_err_correo; return false }
            clean == userEmail.trim().lowercase() -> { shareMessage = R.string.share_err_propio; return false }
            friends.any { it.email == clean } -> { shareMessage = R.string.share_err_repetido; return false }
        }
        friends.add(Friend(clean, name?.trim().orEmpty().ifBlank { clean.substringBefore('@') }))
        saveFriends()
        pushAllowed()
        return true
    }

    fun removeFriend(email: String) {
        friends.removeAll { it.email == email }
        saveFriends()
        pushAllowed()
    }

    fun setFriendEnabled(email: String, enabled: Boolean) {
        val index = friends.indexOfFirst { it.email == email }
        if (index < 0) return
        friends[index] = friends[index].copy(enabled = enabled)
        saveFriends()
        pushAllowed()
    }

    /** Si ya se está compartiendo, la lista de quién puede ver cambia de inmediato. */
    private fun pushAllowed() {
        if (!isSharing) return
        val allowed = enabledEmails()
        viewModelScope.launch {
            try { LocationShareRepository.updateAllowed(getApplication(), allowed) } catch (e: Exception) { /* se reintenta al siguiente cambio */ }
        }
    }

    /** Actualiza "ahora" (para ocultar ubicaciones viejas). Las pantallas lo llaman cada ~15 s. */
    fun tickNow() { nowMs = System.currentTimeMillis() }

    /** Cada pantalla que muestra amigos lo llama al entrar y [stopWatchingFriends] al salir. */
    fun startWatchingFriends() {
        watchers++
        if (watchers != 1 || !isLoggedIn || !LocationShareRepository.isAvailable) return
        stopWatching = LocationShareRepository.observeFriends(
            context = getApplication(),
            myEmail = userEmail,
            onChange = { rawFriendLocations = it; tickNow() },
            onError = { rawFriendLocations = emptyList() }
        )
    }

    fun stopWatchingFriends() {
        if (watchers > 0) watchers--
        if (watchers == 0) {
            stopWatching?.invoke()
            stopWatching = null
            rawFriendLocations = emptyList()
        }
    }

    /** Empieza a compartir con los amigos activados. La pantalla debe haber pedido el permiso de ubicación. */
    fun startSharing(durationMs: Long = DEFAULT_SHARE_DURATION_MS) {
        shareMessage = null
        if (!LocationShareRepository.isAvailable) { shareMessage = R.string.share_err_no_disponible; return }
        if (!isLoggedIn) { shareMessage = R.string.share_err_sin_sesion; return }
        if (enabledEmails().isEmpty()) { shareMessage = R.string.share_err_sin_amigos; return }
        if (isSharing || startingShare) return
        startingShare = true
        val expiresAt = System.currentTimeMillis() + durationMs
        val app = getApplication<Application>()
        viewModelScope.launch {
            try {
                LocationShareRepository.start(app, userName, userEmail, enabledEmails(), expiresAt, userLocation)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                shareMessage = R.string.share_err_red
                startingShare = false
                return@launch
            }
            val started = sharer.start { position ->
                userLocation = position
                val now = System.currentTimeMillis()
                if (now - lastPublishMs >= 3000L) {
                    lastPublishMs = now
                    viewModelScope.launch {
                        try { LocationShareRepository.publish(app, position) } catch (e: Exception) { /* la siguiente lectura reintenta */ }
                    }
                }
            }
            if (!started) {
                shareMessage = R.string.share_err_permiso
                try { LocationShareRepository.stop(app) } catch (e: Exception) { }
                startingShare = false
                return@launch
            }
            startingShare = false
            isSharing = true
            shareExpiresAtMs = expiresAt
            expiryJob?.cancel()
            expiryJob = viewModelScope.launch {
                delay(durationMs)
                stopSharingNow()
            }
        }
    }

    fun stopSharing() { viewModelScope.launch { stopSharingNow() } }

    private suspend fun stopSharingNow() {
        expiryJob?.cancel()
        expiryJob = null
        sharer.stop()
        val wasSharing = isSharing
        isSharing = false
        shareExpiresAtMs = 0L
        if (wasSharing) {
            try { LocationShareRepository.stop(getApplication()) } catch (e: Exception) { /* expira solo por expiresAtMs */ }
        }
    }

    private fun clearShareState() {
        stopWatching?.invoke()
        stopWatching = null
        watchers = 0
        rawFriendLocations = emptyList()
        friends.clear()
        shareMessage = null
    }

    fun isFavorite(name: String): Boolean = name in favs

    fun toggleFavorite(name: String) {
        if (!favs.remove(name)) favs.add(name)
    }

    fun triggerAlertToast() {
        showAlertToast = true
    }

    fun dismissAlertToast() {
        showAlertToast = false
    }
}
