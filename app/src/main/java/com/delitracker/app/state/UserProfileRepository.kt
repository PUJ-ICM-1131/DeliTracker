package com.delitracker.app.state

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AuthFailure(val status: Int, val code: String) : Exception(code)
data class RemoteProfile(val name: String, val email: String, val emailVerificationPending: Boolean = false)

/** La sesión y su renovación son administradas por Firebase Authentication. */
class UserProfileRepository(context: Context, authOverride: FirebaseAuth? = null) {
    private val appContext = context.applicationContext
    private val auth by lazy { authOverride ?: FirebaseAuthProvider.get(appContext) }

    private fun profile(user: FirebaseUser, pending: Boolean = false) = RemoteProfile(
        user.displayName.orEmpty().ifBlank { user.email.orEmpty().substringBefore('@') },
        user.email.orEmpty(), pending
    )

    private suspend fun <T> request(action: suspend () -> T): T = try {
        action()
    } catch (e: CancellationException) {
        throw e
    } catch (e: AuthFailure) {
        throw e
    } catch (e: Exception) {
        throw FirebaseAuthErrors.translate(e)
    }

    suspend fun restore(): RemoteProfile? = request {
        val user = auth.currentUser ?: return@request null
        try {
            user.reload().await()
            user.getIdToken(true).await()
            profile(auth.currentUser ?: return@request null)
        } catch (e: FirebaseAuthException) {
            if (e.errorCode in setOf("ERROR_USER_NOT_FOUND", "ERROR_USER_DISABLED", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN")) {
                auth.signOut()
                null
            } else throw e
        }
    }

    suspend fun authenticate(name: String?, email: String, password: String): RemoteProfile = request {
        val result = if (name == null) {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
        } else {
            auth.createUserWithEmailAndPassword(email.trim(), password).await()
        }
        val user = result.user ?: throw AuthFailure(401, "unauthorized")
        if (name != null) {
            try {
                user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                auth.signOut()
                throw AuthFailure(409, "profile_incomplete")
            }
        }
        profile(user)
    }

    suspend fun updateProfile(name: String, email: String): RemoteProfile = request {
        if (name.isBlank() || name.length > 100 || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            throw AuthFailure(400, "invalid_input")
        }
        val user = auth.currentUser ?: throw AuthFailure(401, "unauthorized")
        val emailChanged = !email.equals(user.email, ignoreCase = true)
        if (emailChanged) user.verifyBeforeUpdateEmail(email).await()
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
        profile(user, pending = emailChanged)
    }

    suspend fun sendPasswordReset(email: String): Unit = request {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) throw AuthFailure(400, "invalid_email")
        try { auth.sendPasswordResetEmail(email.trim()).await() }
        catch (e: FirebaseAuthException) {
            if (e.errorCode != "ERROR_USER_NOT_FOUND") throw e
        }
        Unit
    }

    suspend fun deleteAccount() = request {
        val user = auth.currentUser ?: throw AuthFailure(401, "unauthorized")
        user.delete().await()
        auth.signOut()
    }

    suspend fun logout() = request { auth.signOut() }
}
