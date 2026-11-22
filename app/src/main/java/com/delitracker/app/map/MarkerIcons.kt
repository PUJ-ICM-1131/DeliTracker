package com.delitracker.app.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64

/**
 * Marcadores personalizados del mapa: un círculo con la foto de perfil y un aro de color.
 * Si la persona no tiene foto se dibuja su inicial sobre el color (nunca el ícono por defecto).
 */
internal object MarkerIcons {

    private const val SIZE_DP = 44
    private const val RING_DP = 3

    fun decodeBase64(photo: String?): Bitmap? {
        if (photo.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(photo, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun decodeFile(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 200) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun circular(context: Context, photo: Bitmap?, initial: String, color: Int): Drawable {
        val density = context.resources.displayMetrics.density
        val size = (SIZE_DP * density).toInt()
        val ring = RING_DP * density
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val center = size / 2f

        paint.color = color
        canvas.drawCircle(center, center, center, paint)

        val innerRadius = center - ring
        if (photo != null) {
            val side = minOf(photo.width, photo.height)
            val square = Bitmap.createBitmap(photo, (photo.width - side) / 2, (photo.height - side) / 2, side, side)
            val inner = (innerRadius * 2).toInt()
            val scaled = Bitmap.createScaledBitmap(square, inner, inner, true)
            val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shader.setLocalMatrix(Matrix().apply { setTranslate(ring, ring) })
            paint.shader = shader
            canvas.drawCircle(center, center, innerRadius, paint)
            paint.shader = null
        } else {
            canvas.drawCircle(center, center, innerRadius, paint)
            paint.color = Color.WHITE
            paint.textSize = size * 0.45f
            paint.textAlign = Paint.Align.CENTER
            paint.isFakeBoldText = true
            val letter = initial.trim().take(1).uppercase().ifEmpty { "?" }
            canvas.drawText(letter, center, center - (paint.descent() + paint.ascent()) / 2f, paint)
        }
        return BitmapDrawable(context.resources, output)
    }
}
