package com.delitracker.app.notifications

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.test.platform.app.InstrumentationRegistry
import com.delitracker.app.data.RestaurantCatalog
import com.delitracker.app.share.FriendLocation
import com.delitracker.app.route.LatLon
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val account = "notifications-test@example.invalid"
    private lateinit var notifications: DeliNotifications
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    @Before fun setup() {
        context.getSharedPreferences("notifications", Context.MODE_PRIVATE).edit().clear().commit()
        notifications = DeliNotifications(context)
        notifications.clear()
        if (Build.VERSION.SDK_INT >= 33 && InstrumentationRegistry.getArguments().getString("permissionDenied") != "true") {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS"
            ).let { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() } }
            val deadline = android.os.SystemClock.elapsedRealtime() + 5000
            while (!notifications.systemEnabled() && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
            assertTrue(notifications.systemEnabled())
        }
        notifications.setEnabled(account, DeliNotifications.NEARBY, true)
        notifications.setEnabled(account, DeliNotifications.SOCIAL, true)
        assertCount(0)
    }

    @After fun cleanup() {
        notifications.clear()
        context.getSharedPreferences("notifications", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun assertCount(expected: Int) {
        Thread.sleep(200)
        val deadline = android.os.SystemClock.elapsedRealtime() + 10_000
        while (manager.activeNotifications.size != expected && android.os.SystemClock.elapsedRealtime() < deadline) {
            Thread.sleep(100)
        }
        assertEquals(expected, manager.activeNotifications.size)
    }

    @Test fun distancePreferencesAndCooldownBoundaries() {
        assertTrue(NotificationPolicy.nearby(200.0, true, 1, null))
        assertFalse(NotificationPolicy.nearby(200.1, true, 1, null))
        assertFalse(NotificationPolicy.nearby(Double.NaN, true, 1, null))
        assertFalse(NotificationPolicy.nearby(-1.0, true, 1, null))
        assertFalse(NotificationPolicy.nearby(10.0, false, 1, null))
        assertFalse(NotificationPolicy.nearby(10.0, true, 3_599_999, 0))
        assertTrue(NotificationPolicy.nearby(10.0, true, 3_600_000, 0))
        assertTrue(NotificationPolicy.cuisineMatches("Italian", "Italiana"))
        assertTrue(NotificationPolicy.cuisineMatches("Vegan", "Vegana"))
        assertFalse(NotificationPolicy.cuisineMatches("Cafe", "Vegana"))
    }

    @Test fun nearbyPostsRealSystemNotificationAndPersistsCooldown() {
        val restaurant = RestaurantCatalog.all.first()
        notifications.nearby(account, listOf(restaurant to 80.0)) { true }
        assertCount(1)
        val posted = manager.activeNotifications.single().notification
        assertNotNull(posted.contentIntent)
        assertTrue(posted.extras.getCharSequence("android.text").toString().contains("80"))
        notifications.clear()
        DeliNotifications(context).nearby(account, listOf(restaurant to 80.0)) { true }
        assertCount(0)
    }

    @Test fun disabledAccountAndNonMatchingRestaurantDoNotNotify() {
        assertFalse(notifications.test("other-account@example.invalid"))
        notifications.nearby(account, listOf(RestaurantCatalog.all.first() to 100.0)) { false }
        assertCount(0)
        notifications.setEnabled(account, DeliNotifications.NEARBY, false)
        assertFalse(DeliNotifications(context).enabled(account, DeliNotifications.NEARBY))
        assertFalse(notifications.test(account))
        assertCount(0)
    }

    @Test fun androidBlockDoesNotConsumeNearbyEvent() {
        val denied = InstrumentationRegistry.getArguments().getString("permissionDenied") == "true"
        val candidates = listOf(RestaurantCatalog.all.first() to 80.0)
        if (denied) {
            assertFalse(notifications.systemEnabled())
            assertFalse(notifications.test(account))
            notifications.nearby(account, candidates) { true }
            assertCount(0)
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS"
            ).let { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() } }
            val deadline = android.os.SystemClock.elapsedRealtime() + 5000
            while (!notifications.systemEnabled() && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
            assertTrue(notifications.systemEnabled())
        }
        notifications.nearby(account, candidates) { true }
        assertCount(1)
    }

    @Test fun socialRejectsExpiredStaleAndRepeatedSessions() {
        val now = System.currentTimeMillis()
        val friend = FriendLocation("friend-test", "Amigo de prueba", "friend@example.invalid", LatLon(4.67, -74.05), now, now + 3_600_000)
        notifications.friends(account, listOf(friend.copy(updatedAtMs = now - 120_001)))
        notifications.friends(account, listOf(friend.copy(expiresAtMs = now - 1)))
        assertCount(0)
        notifications.friends(account, listOf(friend))
        assertCount(1)
        notifications.clear()
        DeliNotifications(context).friends(account, listOf(friend.copy(updatedAtMs = now + 1)))
        assertCount(0)
        notifications.friends(account, listOf(friend.copy(expiresAtMs = now + 3_700_000)))
        assertCount(1)
    }

    @Test fun disablingNearbyPreservesSocialNotifications() {
        notifications.test(account)
        val now = System.currentTimeMillis()
        notifications.friends(account, listOf(FriendLocation("test", "Amigo de prueba", "friend@example.invalid", LatLon(4.67, -74.05), now, now + 3_600_000)))
        assertCount(2)
        notifications.setEnabled(account, DeliNotifications.NEARBY, false)
        assertCount(1)
        if (Build.VERSION.SDK_INT >= 26) {
            assertEquals(DeliNotifications.SOCIAL, manager.activeNotifications.single().notification.channelId)
        }
    }

    @Test fun targetRetainsAccountAndRestaurant() {
        val intent = Intent().putExtra("notification_account", account)
            .putExtra("notification_restaurant", """{"id":"api-restaurant","name":"Restaurante de prueba","lat":4.67,"lon":-74.05,"cuisine":"Vegan"}""")
        val target = DeliNotifications.target(intent)!!
        assertEquals(account, target.account)
        assertEquals("api-restaurant", target.restaurant!!.id)
        assertEquals(4.67, target.restaurant!!.position.lat, 0.0001)
        assertNull(DeliNotifications.target(Intent()))
        assertNull(DeliNotifications.target(intent.putExtra("notification_restaurant", "invalid")))
    }

    @Test fun captureNotificationProof() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(com.delitracker.app.MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    com.delitracker.app.ui.theme.DeliTrackerTheme {
                        androidx.compose.material3.Surface(modifier = androidx.compose.ui.Modifier.systemBarsPadding()) {
                            NotificationSettingsScreen(account)
                        }
                    }
                }
            }
            Thread.sleep(1500)
            val folder = java.io.File(context.getExternalFilesDir(null), "notification-proof").apply { mkdirs() }
            fun screenshot(name: String) {
                val bitmap = instrumentation.uiAutomation.takeScreenshot()
                java.io.File(folder, name).outputStream().use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
            screenshot("settings.png")
            notifications.nearby(account, listOf(RestaurantCatalog.all[1] to 80.0)) { true }
            val now = System.currentTimeMillis()
            notifications.friends(account, listOf(FriendLocation("proof-friend", "Amigo de prueba", "friend@example.invalid", LatLon(4.67, -74.05), now, now + 3_600_000)))
            assertCount(2)
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            Thread.sleep(1000)
            screenshot("notifications.png")
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        }
    }
}
