package com.mybetlive.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.mybetlive.R
import com.mybetlive.data.BetProgress
import com.mybetlive.data.ProgressTone
import com.mybetlive.data.display

/** Shared drawing for the Compose card and the notification's RemoteViews bitmap. */
object ProgressTrackRenderer {
    fun notificationIcon(sport: String): Int = when(sport) {
        "NFL" -> R.drawable.ic_notification_football
        "SOCCER" -> R.drawable.ic_notification_soccer
        "MLB" -> R.drawable.ic_notification_baseball
        else -> R.drawable.ic_notification_ball
    }
    fun sportIcon(sport: String): Int = when (sport) {
        "SOCCER" -> R.drawable.ic_soccer
        "MLB" -> R.drawable.ic_baseball
        "NBA" -> R.drawable.ic_basketball
        "NHL" -> R.drawable.ic_hockey
        else -> R.drawable.ic_football
    }

    fun bitmap(context: Context, progress: BetProgress, compact: Boolean = false): Bitmap {
        val height = if (compact) 72 else 192
        val bitmap = Bitmap.createBitmap(960, height, Bitmap.Config.ARGB_8888)
        draw(context, Canvas(bitmap), 960f, height.toFloat(), progress, 0xFF9AAEBB.toInt(), compact)
        return bitmap
    }

    fun draw(context: Context, canvas: Canvas, width: Float, height: Float, progress: BetProgress,
        labelColor: Int, compact: Boolean = false, position: Float = progress.fraction) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val iconSize = height * if (compact) .85f else .38f
        val inset = iconSize / 2 + 3f
        val left = inset
        val right = width - inset
        val centerY = height * if (compact) .5f else .52f
        val x = left + (right - left) * position.coerceIn(0f, 1f)
        val goalX = left + (right - left) * progress.targetFraction
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = height * if (compact) .17f else .10f
        paint.color = 0xFF7F8B94.toInt()
        paint.alpha = 80
        canvas.drawLine(left, centerY, right, centerY, paint)
        if (progress.centered && progress.tone != ProgressTone.MUTED) {
            paint.alpha = 75
            paint.color = ProgressTone.RED.argb
            canvas.drawLine(left, centerY, goalX, centerY, paint)
            paint.color = ProgressTone.GREEN.argb
            canvas.drawLine(goalX, centerY, right, centerY, paint)
        }
        paint.alpha = 255
        paint.color = progress.tone.argb
        canvas.drawLine(if (progress.centered) goalX else left, centerY, x, centerY, paint)
        paint.strokeWidth = height * .025f
        paint.color = labelColor
        canvas.drawLine(goalX, centerY - height * .13f, goalX, centerY + height * .13f, paint)
        val icon = ContextCompat.getDrawable(context, sportIcon(progress.sport))?.mutate()
        icon?.setBounds((x - iconSize / 2).toInt(), (centerY - iconSize / 2).toInt(),
            (x + iconSize / 2).toInt(), (centerY + iconSize / 2).toInt())
        icon?.draw(canvas)
        if (compact) return
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = height * .23f
        paint.color = progress.tone.argb
        paint.textAlign = Paint.Align.CENTER
        val current = progress.current.display()
        val halfText = paint.measureText(current) / 2
        canvas.drawText(com.mybetlive.i18n.AppStrings.translate(current), x.coerceIn(halfText + 2, width - halfText - 2), height * .22f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = height * .19f
        paint.color = labelColor
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(com.mybetlive.i18n.AppStrings.translate(progress.start.display()), left, height * .95f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(com.mybetlive.i18n.AppStrings.translate(progress.end.display()), right, height * .95f, paint)
        if (progress.centered || progress.targetFraction in .18f.. .82f) {
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = height * .15f
            canvas.drawText(com.mybetlive.i18n.AppStrings.translate(if (progress.centered) "0" else "Línea ${progress.target.display()}"), goalX, height * .95f, paint)
        }
    }
}
