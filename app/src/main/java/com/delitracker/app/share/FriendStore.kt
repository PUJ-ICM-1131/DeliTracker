package com.delitracker.app.share

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Lista de amigos guardada solo en este dispositivo, separada por cuenta (correo del usuario). */
class FriendStore(context: Context) {
    private val prefs = context.getSharedPreferences("friends", Context.MODE_PRIVATE)
    private fun key(owner: String) = "friends_" + owner.trim().lowercase()

    fun load(owner: String): List<Friend> {
        if (owner.isBlank()) return emptyList()
        val raw = prefs.getString(key(owner), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val email = item.optString("email").trim().lowercase()
                if (email.isEmpty()) null
                else Friend(email, item.optString("name").ifBlank { email.substringBefore('@') }, item.optBoolean("enabled", true))
            }
        } catch (e: org.json.JSONException) {
            emptyList()
        }
    }

    fun save(owner: String, friends: List<Friend>) {
        if (owner.isBlank()) return
        val array = JSONArray()
        friends.forEach {
            array.put(JSONObject().put("email", it.email).put("name", it.name).put("enabled", it.enabled))
        }
        prefs.edit().putString(key(owner), array.toString()).apply()
    }
}
