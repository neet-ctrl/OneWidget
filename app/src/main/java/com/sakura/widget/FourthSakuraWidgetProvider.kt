package com.sakura.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

class FourthSakuraWidgetProvider : BaseSakuraWidgetProvider() {
    override val artworkResId = R.drawable.widget_art_fourth
    override val layoutResId = R.layout.widget_sakura_fourth
    override val designWidth = 1280f
    override val designHeight = 720f
    override val preserveArtworkAspect = true

    override fun drawDynamicContent(
        canvas: Canvas,
        time: String,
        meridiem: String,
        date: String,
        dateWithYear: String,
        weather: WeatherRepository.Reading,
    ) {
        drawClockFaceDate(canvas, time, date)
    }

    private fun drawClockFaceDate(canvas: Canvas, time: String, date: String) {
        val centerX = designWidth * 0.5f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(177, 76, 86)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        textPaint.textSize = designWidth * 0.060f
        canvas.drawText(
            time.removePrefix("0"),
            centerX,
            designHeight * 0.715f,
            textPaint,
        )

        textPaint.textSize = designWidth * 0.043f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        canvas.drawText(
            monthDay(date),
            centerX,
            designHeight * 0.800f,
            textPaint,
        )
    }

    private fun monthDay(date: String): String {
        val dayMonth = date.substringAfter(", ", date).split(" ", limit = 2)
        return if (dayMonth.size == 2) {
            "${dayMonth[1]} ${dayMonth[0]}"
        } else {
            date
        }
    }
}