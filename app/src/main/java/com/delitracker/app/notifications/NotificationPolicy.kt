package com.delitracker.app.notifications


internal object NotificationPolicy {
    const val RADIUS_METERS = 200.0
    const val COOLDOWN_MS = 60 * 60 * 1000L

    fun nearby(distance: Double, matches: Boolean, now: Long, last: Long?): Boolean =
        matches && distance.isFinite() && distance >= 0 && distance <= RADIUS_METERS &&
            (last == null || now - last >= COOLDOWN_MS)

    fun freshShare(now: Long, updated: Long, expires: Long): Boolean =
        expires > now && updated <= now + 30_000 && now - updated < 120_000

    fun cuisineMatches(actual: String, selected: String): Boolean {
        fun normalized(value: String): String = when (value.trim().lowercase()) {
            "italian", "italiana" -> "italian"
            "mexican", "mexicana" -> "mexican"
            "vegan", "vegana" -> "vegan"
            "gluten free", "sin gluten" -> "glutenfree"
            else -> value.trim().lowercase()
        }
        return actual.split(';', ',').any { normalized(it) == normalized(selected) }
    }
}
