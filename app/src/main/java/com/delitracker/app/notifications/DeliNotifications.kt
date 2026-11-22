package com.delitracker.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.delitracker.app.MainActivity
import com.delitracker.app.R
import com.delitracker.app.data.Restaurant
import com.delitracker.app.data.displayName
import com.delitracker.app.share.FriendLocation
import org.json.JSONObject
import java.security.MessageDigest

data class NotificationTarget(val account: String, val restaurant: Restaurant?)


class DeliNotifications(private val context: Context) {
    private val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)

    init { createChannels() }

    private fun key(account: String, suffix: String): String =
        MessageDigest.getInstance("SHA-256").digest(account.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) } + ":" + suffix

    fun enabled(account: String, category: String): Boolean =
        account.isNotBlank() && prefs.getBoolean(key(account, category), false)

    fun setEnabled(account: String, category: String, enabled: Boolean) {
        prefs.edit().putBoolean(key(account, category), enabled).apply()
        if (!enabled) {
            prefs.getStringSet(key(account, "posted:$category"), emptySet()).orEmpty()
                .forEach { NotificationManagerCompat.from(context).cancel(it, 0) }
            val prefix = key(account, "$category:")
            context.getSystemService(NotificationManager::class.java).activeNotifications
                .filter { it.tag?.startsWith(prefix) == true }
                .forEach { NotificationManagerCompat.from(context).cancel(it.tag, it.id) }
        }
    }

    fun systemEnabled(channel: String? = null): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || channel == null ||
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(channel)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(
            NotificationChannel(NEARBY, context.getString(R.string.notif_nearby), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(SOCIAL, context.getString(R.string.notif_social), NotificationManager.IMPORTANCE_DEFAULT)
        ))
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun post(account: String, channel: String, id: String, title: String, body: String,
                     restaurant: Restaurant? = null): Boolean {
        if (!enabled(account, channel) || !systemEnabled(channel)) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ACCOUNT, account)
            if (restaurant != null) putExtra(EXTRA_RESTAURANT, JSONObject().apply {
                put("id", restaurant.id); put("name", restaurant.displayName(context))
                put("lat", restaurant.position.lat); put("lon", restaurant.position.lon)
                put("cuisine", restaurant.cuisine ?: if (restaurant.cuisineRes != 0) context.getString(restaurant.cuisineRes) else "")
                put("address", restaurant.address); put("phone", restaurant.phone)
                put("website", restaurant.website); put("hours", restaurant.openingHours)
                put("parking", restaurant.hasParking)
            }.toString())
        }
        intent.data = android.net.Uri.parse("delitracker://notification/" + key(account, "$channel:$id"))
        val pending = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        return try {
            val tag = key(account, "$channel:$id")
            NotificationManagerCompat.from(context).notify(tag, 0, notification)
            val postedKey = key(account, "posted:$channel")
            val tags = prefs.getStringSet(postedKey, emptySet()).orEmpty().toMutableSet()
            tags.add(tag)
            prefs.edit().putStringSet(postedKey, tags).apply()
            true
        } catch (_: SecurityException) { false }
    }

    fun nearby(account: String, candidates: List<Pair<Restaurant, Double>>, matches: (Restaurant) -> Boolean) {
        val now = System.currentTimeMillis()
        val global = key(account, "lastNearby")
        if (prefs.contains(global) && now - prefs.getLong(global, 0) < 5 * 60_000) return
        for ((restaurant, distance) in candidates.sortedBy { it.second }) {
            val event = key(account, "near:" + restaurant.id)
            val last = if (prefs.contains(event)) prefs.getLong(event, 0) else null
            if (!NotificationPolicy.nearby(distance, matches(restaurant), now, last)) continue
            if (post(account, NEARBY, restaurant.id, context.getString(R.string.notif_nearby_title),
                    context.getString(R.string.notif_nearby_body, restaurant.displayName(context), distance.toInt()), restaurant)) {
                prefs.edit().putLong(event, now).putLong(global, now).apply()
            }
            break
        }
    }

    fun friends(account: String, friends: List<FriendLocation>) {
        val now = System.currentTimeMillis()
        for (friend in friends) {
            if (!NotificationPolicy.freshShare(now, friend.updatedAtMs, friend.expiresAtMs)) continue
            val event = key(account, "share:" + friend.uid)
            if (prefs.getLong(event, 0) == friend.expiresAtMs) continue
            if (post(account, SOCIAL, friend.uid, context.getString(R.string.notif_social_title),
                    context.getString(R.string.notif_social_body, friend.name))) {
                prefs.edit().putLong(event, friend.expiresAtMs).apply()
            }
        }
    }

    fun test(account: String): Boolean = post(account, NEARBY, "test",
        context.getString(R.string.notif_test_title), context.getString(R.string.notif_test_body))

    fun clear() { NotificationManagerCompat.from(context).cancelAll() }

    companion object {
        const val NEARBY = "nearby"
        const val SOCIAL = "social"
        private const val EXTRA_ACCOUNT = "notification_account"
        private const val EXTRA_RESTAURANT = "notification_restaurant"

        fun target(intent: Intent): NotificationTarget? {
            val account = intent.getStringExtra(EXTRA_ACCOUNT) ?: return null
            val restaurant = try {
                intent.getStringExtra(EXTRA_RESTAURANT)?.let { raw ->
                    val json = JSONObject(raw)
                    val lat = json.getDouble("lat")
                    val lon = json.getDouble("lon")
                    require(lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0)
                    Restaurant(id = json.getString("id"), name = json.getString("name"),
                        cuisine = json.optString("cuisine"),
                        address = json.optString("address").ifBlank { null },
                        phone = json.optString("phone").ifBlank { null },
                        website = json.optString("website").ifBlank { null },
                        openingHours = json.optString("hours").ifBlank { null },
                        hasParking = json.optBoolean("parking"),
                        position = com.delitracker.app.route.LatLon(lat, lon))
                }
            } catch (_: Exception) { return null }
            return NotificationTarget(account, restaurant)
        }
    }
}
