package com.delitracker.app.places

import android.content.Context

/** Últimas búsquedas del usuario, guardadas en el dispositivo (SharedPreferences). Máximo 5. */
object RecentSearches {

    private const val PREFS = "delitracker_search"
    private const val KEY = "recent"
    private const val MAX = 5

    fun load(context: Context): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "").orEmpty()
            .split('\n').filter { it.isNotBlank() }

    /** Agrega [text] al inicio, sin repetirlo, y devuelve la lista actualizada. */
    fun add(context: Context, text: String): List<String> {
        val clean = text.trim().replace('\n', ' ')
        if (clean.length < 2) return load(context)
        val updated = (listOf(clean) + load(context).filterNot { it.equals(clean, ignoreCase = true) }).take(MAX)
        save(context, updated)
        return updated
    }

    fun clear(context: Context) = save(context, emptyList())

    private fun save(context: Context, items: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, items.joinToString("\n")).apply()
    }
}
