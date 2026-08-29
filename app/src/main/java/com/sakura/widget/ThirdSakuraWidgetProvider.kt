package com.sakura.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

class ThirdSakuraWidgetProvider : BaseSakuraWidgetProvider() {
    override val artworkResId = R.drawable.widget_art_third
    override val layoutResId = R.layout.widget_sakura_third
    override val designWidth = 1280f
    override val designHeight = 640f
    override val preserveArtworkAspect = true

    override fun drawDynamicContent(
        canvas: Canvas,
        time: String,
        meridiem: String,
        date: String,
        dateWithYear: String,
        weather: WeatherRepository.Reading,
    ) {
        drawClock(canvas, time)
        drawDate(canvas, date, dateWithYear)
        drawWeather(canvas, weather)
    }

    private fun drawClock(canvas: Canvas, time: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(194, 91, 122)
            textSize = designWidth * 0.145f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            letterSpacing = 0.01f
        }
        val left = designWidth * 0.115f
        canvas.drawText(time, left, designHeight * 0.50f, paint)
    }

    private fun drawDate(canvas: Canvas, dayDate: String, date: String) {
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(194, 91, 122)
            textSize = designWidth * 0.078f
            typeface = Typeface.create("serif", Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            currentDayFromDate(dayDate),
            designWidth * 0.245f,
            designHeight * 0.57f,
            dayPaint,
        )

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(177, 76, 107)
            textSize = designWidth * 0.031f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val centerX = designWidth * 0.31f
        val centerY = designHeight * 0.778f
        canvas.drawText(
            date,
            centerX,
            centerY - centeredBaselineOffset(datePaint),
            datePaint,
        )
    }

    private fun currentDayFromDate(date: String): String =
        date.substringBefore(", ").ifEmpty { date }

    private fun drawWeather(canvas: Canvas, weather: WeatherRepository.Reading) {
        val temperaturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(62, 65, 71)
            textSize = designWidth * 0.052f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val conditionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(62, 65, 71)
            textSize = designWidth * 0.025f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val x = designWidth * 0.685f
        canvas.drawText(weather.temperature, x, designHeight * 0.49f, temperaturePaint)
        canvas.drawText(weather.condition, x, designHeight * 0.56f, conditionPaint)
    }
}