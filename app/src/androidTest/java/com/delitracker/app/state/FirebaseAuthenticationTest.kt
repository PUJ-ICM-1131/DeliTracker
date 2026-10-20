package com.delitracker.app.state

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.delitracker.app.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseAuthenticationTest {
    @Test
    fun registrationLoginRestoreProfileResetAndDelete() = runBlocking {
        check(BuildConfig.DEBUG && BuildConfig.FIREBASE_AUTH_EMULATOR) { "Esta prueba solo se ejecuta contra Firebase local" }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val auth = FirebaseAuthProvider.get(context)
        assertEquals("demo-delitracker", auth.app.options.projectId)
        auth.signOut()
        val repository = UserProfileRepository(context)
        val email = "demo-${UUID.randomUUID()}@example.com"
        val password = "DemoSeguro2026!"
        assertNull(repository.restore())
        try {
            val created = repository.authenticate("Cuenta de prueba", email, password)
            assertEquals("Cuenta de prueba", created.name)
            assertEquals(email, created.email)
            expectFailure("email_exists") { repository.authenticate("Duplicada", email, password) }
            repository.logout()
            assertNull(repository.restore())
            expectFailure("invalid_credentials") { repository.authenticate(null, email, "Incorrecta123!") }
            assertEquals(email, repository.authenticate(null, email, password).email)
            assertEquals(email, UserProfileRepository(context).restore()?.email)
            assertEquals("Nombre actualizado", repository.updateProfile("Nombre actualizado", email).name)
            repository.sendPasswordReset(email)
            expectFailure("invalid_email") { repository.sendPasswordReset("correo-invalido") }
            repository.deleteAccount()
            assertNull(repository.restore())
            expectFailure("invalid_credentials") { repository.authenticate(null, email, password) }
        } finally {
            if (auth.currentUser?.email == email) repository.deleteAccount()
            auth.signOut()
        }
    }

    private suspend fun expectFailure(code: String, action: suspend () -> Unit) {
        try { action(); fail("La operación debía rechazarse: $code") }
        catch (e: AuthFailure) { assertEquals(code, e.code) }
    }
}
