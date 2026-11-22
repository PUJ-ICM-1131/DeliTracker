package com.delitracker.app.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.delitracker.app.share.LocationShareRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseNotificationsTest {
    @Test fun realUsersReceiveSharedLocationNotification() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("cloudTest") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = FirebaseApp.getInstance()
        val receiver = FirebaseAuth.getInstance(app)
        assertNull("Esta prueba requiere un emulador sin sesión previa", receiver.currentUser)
        val suffix = UUID.randomUUID().toString()
        val senderApp = FirebaseApp.initializeApp(context, app.options, "notification-test-$suffix")
        val sender = FirebaseAuth.getInstance(senderApp)
        val db = FirebaseFirestore.getInstance(senderApp)
        val notifications = DeliNotifications(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        var stop: (() -> Unit)? = null
        var document: com.google.firebase.firestore.DocumentReference? = null
        try {
            val password = UUID.randomUUID().toString() + "Aa1!"
            val senderUser = Tasks.await(sender.createUserWithEmailAndPassword(
                "notification-sender-$suffix@example.invalid", password), 30, TimeUnit.SECONDS).user!!
            val receiverUser = Tasks.await(receiver.createUserWithEmailAndPassword(
                "notification-receiver-$suffix@example.invalid", password), 30, TimeUnit.SECONDS).user!!
            val email = receiverUser.email!!
            notifications.clear()
            notifications.setEnabled(email, DeliNotifications.SOCIAL, true)
            val received = CountDownLatch(1)
            var listenerFailed = false
            stop = LocationShareRepository.observeFriends(context, email, { friends ->
                if (friends.any { it.uid == senderUser.uid }) {
                    notifications.friends(email, friends)
                    received.countDown()
                }
            }, { listenerFailed = true; received.countDown() })
            assertNotNull(stop)
            val now = System.currentTimeMillis()
            document = db.collection("shares").document(senderUser.uid)
            Tasks.await(document.set(mapOf(
                "ownerName" to "Emisor de prueba", "ownerEmail" to senderUser.email,
                "active" to true, "allowed" to listOf(email),
                "lat" to 4.6752, "lon" to -74.0495,
                "updatedAtMs" to now, "expiresAtMs" to now + 3_600_000
            )), 30, TimeUnit.SECONDS)
            assertTrue("El receptor no recibió el evento en tiempo real", received.await(30, TimeUnit.SECONDS))
            assertFalse("Firebase rechazó la escucha del receptor", listenerFailed)
            val deadline = android.os.SystemClock.elapsedRealtime() + 10_000
            while (manager.activeNotifications.isEmpty() && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
            val posted = manager.activeNotifications.single().notification
            assertTrue(posted.extras.getCharSequence("android.text").toString().contains("Emisor de prueba"))
        } finally {
            stop?.invoke()
            document?.let { try { Tasks.await(it.delete(), 20, TimeUnit.SECONDS) } catch (_: Exception) {} }
            sender.currentUser?.let { try { Tasks.await(it.delete(), 20, TimeUnit.SECONDS) } catch (_: Exception) {} }
            receiver.currentUser?.let { try { Tasks.await(it.delete(), 20, TimeUnit.SECONDS) } catch (_: Exception) {} }
            sender.signOut()
            receiver.signOut()
            notifications.clear()
            context.getSharedPreferences("notifications", Context.MODE_PRIVATE).edit().clear().commit()
            senderApp.delete()
        }
    }
}
