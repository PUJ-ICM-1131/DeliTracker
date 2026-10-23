package com.delitracker.app.reviews

/** Reseña de un usuario sobre un restaurante. [photo] es un JPEG comprimido (opcional). */
data class Review(
    val uid: String,
    val userName: String,
    val stars: Int,
    val text: String,
    val photo: ByteArray?,
    val createdAtMs: Long
)

/** Resumen de las reseñas de un restaurante. */
data class ReviewSummary(val average: Double, val count: Int)

fun List<Review>.summary(): ReviewSummary? =
    if (isEmpty()) null else ReviewSummary(map { it.stars }.average(), size)
