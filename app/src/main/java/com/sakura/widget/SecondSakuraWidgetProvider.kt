package com.sakura.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.util.Locale

class SecondSakuraWidgetProvider : BaseSakuraWidgetProvider() {
    override val artworkResId = R.drawable.widget_art_second
    override val layoutResId = R.layout.widget_sakura_second
    override val designWidth = 1280f
    override val designHeight = 835f

    override fun drawDynamicContent(
        canvas: Canvas,
        time: String,
        meridiem: String,
        date: String,
        dateWithYear: String,
        weather: WeatherRepository.Reading,
    ) {
        drawClock(canvas, time, meridiem)
        drawDate(canvas, date)
        drawWeather(canvas, weather)
    }

    private fun drawClock(canvas: Canvas, time: String, meridiem: String) {
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(202, 79, 124)
            textSize = designWidth * 0.145f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setShadowLayer(1.5f, 0f, 1f, 0x40FFFFFF)
        }
        val meridiemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(202, 79, 124)
            textSize = designWidth * 0.027f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val parts = time.split(":", limit = 2)
        val hours = parts.getOrElse(0) { time }
        val minutes = parts.getOrElse(1) { "" }
        val left = designWidth * 0.153f
        canvas.drawText(hours, left, designHeight * 0.425f, timePaint)
        canvas.drawText(minutes, left, designHeight * 0.685f, timePaint)
        canvas.drawText(
            meridiem,
            designWidth * 0.365f,
            designHeight * 0.475f,
            meridiemPaint,
        )
    }

    private fun drawDate(canvas: Canvas, date: String) {
        val parts = date.split(", ", limit = 2)
        val day = parts.getOrElse(0) { date }.uppercase(Locale.ENGLISH)
        val monthDate = parts.getOrElse(1) { "" }.uppercase(Locale.ENGLISH)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(196, 76, 119)
            textSize = designWidth * 0.025f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val left = designWidth * 0.153f
        canvas.drawText(day, left, designHeight * 0.765f, paint)
        canvas.drawText(monthDate, left, designHeight * 0.835f, paint)
    }

    private fun drawWeather(canvas: Canvas, weather: WeatherRepository.Reading) {
        val temperaturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(62, 65, 71)
            textSize = designWidth * 0.046f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val conditionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(62, 65, 71)
            textSize = designWidth * 0.020f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val x = designWidth * 0.695f
        canvas.drawText(weather.temperature, x, designHeight * 0.745f, temperaturePaint)
        canvas.drawText(weather.condition, x, designHeight * 0.815f, conditionPaint)
    }
}