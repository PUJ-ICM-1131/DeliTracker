package com.delitracker.app.state

import android.content.Context
import com.delitracker.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AuthFailure(val status: Int, val code: String) : Exception(code)
data class RemoteProfile(val name: String, val email: String)

/** Las credenciales se validan exclusivamente en el backend. No guarda contraseñas. */
class UserProfileRepository(context: Context) {
    private val session = context.getSharedPreferences("backend_session", Context.MODE_PRIVATE)
    private var token: String? = session.getString("token", null)

    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = URL(BuildConfig.AUTH_BASE_URL + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.setRequestProperty("Accept", "application/json")
                token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val json = JSONObject(stream?.bufferedReader()?.use { it.readText() } ?: "{}")
                if (status !in 200..299) throw AuthFailure(status, json.optString("error", "server_error"))
                json
            } finally { connection.disconnect() }
        }

    private fun profile(json: JSONObject): RemoteProfile {
        val user = json.getJSONObject("user")
        return RemoteProfile(user.getString("name"), user.getString("email"))
    }

    suspend fun restore(): RemoteProfile? {
        if (token == null) return null
        return try { profile(request("GET", "/auth/me")) }
        catch (e: AuthFailure) { if (e.status == 401) { clear(); null } else throw e }
    }

    suspend fun authenticate(name: String?, email: String, password: String): RemoteProfile {
        val body = JSONObject().put("email", email).put("password", password)
        if (name != null) body.put("name", name)
        val result = request("POST", if (name == null) "/auth/login" else "/auth/register", body)
        token = result.getString("token")
        session.edit().putString("token", token).commit()
        return profile(result)
    }

    suspend fun updateProfile(name: String, email: String): RemoteProfile =
        profile(request("PATCH", "/auth/me", JSONObject().put("name", name).put("email", email)))

    suspend fun deleteAccount() { request("DELETE", "/auth/me"); clear() }
    suspend fun logout() { try { request("POST", "/auth/logout", JSONObject()) } finally { clear() } }
    private fun clear() { token = null; session.edit().clear().commit() }
}
