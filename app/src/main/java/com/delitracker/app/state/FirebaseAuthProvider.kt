package com.delitracker.app.state

import android.content.Context
import com.delitracker.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

internal object FirebaseAuthProvider {
    private var instance: FirebaseAuth? = null

    @Synchronized
    fun get(context: Context): FirebaseAuth {
        instance?.let { return it }
        val auth = if (BuildConfig.DEBUG && BuildConfig.FIREBASE_AUTH_EMULATOR) {
            val name = "delitracker-auth-emulator"
            val app = FirebaseApp.getApps(context).firstOrNull { it.name == name }
                ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                    .setProjectId("demo-delitracker")
                    .setApplicationId("1:1234567890:android:0000000000000000")
                    .setApiKey("demo-api-key")
                    .build(), name)
            FirebaseAuth.getInstance(app).also {
                it.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 9099)
            }
        } else {
            val app = FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                ?: FirebaseApp.initializeApp(context)
                ?: throw AuthFailure(503, "firebase_not_configured")
            FirebaseAuth.getInstance(app)
        }
        instance = auth
        return auth
    }
}

internal object FirebaseAuthErrors {
    fun translate(e: Exception): AuthFailure = when (e) {
        is FirebaseNetworkException -> AuthFailure(503, "network_error")
        is FirebaseTooManyRequestsException -> AuthFailure(429, "rate_limited")
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> AuthFailure(409, "email_exists")
            "ERROR_INVALID_EMAIL" -> AuthFailure(400, "invalid_email")
            "ERROR_WEAK_PASSWORD", "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS" -> AuthFailure(400, "weak_password")
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS", "ERROR_USER_NOT_FOUND" -> AuthFailure(401, "invalid_credentials")
            "ERROR_REQUIRES_RECENT_LOGIN" -> AuthFailure(403, "recent_login_required")
            "ERROR_USER_DISABLED", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" -> AuthFailure(401, "unauthorized")
            "ERROR_OPERATION_NOT_ALLOWED" -> AuthFailure(503, "provider_disabled")
            "ERROR_API_NOT_AVAILABLE", "ERROR_INVALID_API_KEY", "ERROR_APP_NOT_AUTHORIZED" -> AuthFailure(503, "firebase_not_configured")
            else -> AuthFailure(500, "server_error")
        }
        else -> AuthFailure(500, "server_error")
    }
}
