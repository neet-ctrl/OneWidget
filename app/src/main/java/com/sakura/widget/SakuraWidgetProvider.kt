package com.sakura.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

class SakuraWidgetProvider : BaseSakuraWidgetProvider() {
    override val artworkResId = R.drawable.widget_art_clean
    override val layoutResId = R.layout.widget_sakura
    override val designWidth = 1280f
    override val designHeight = 563f

    override fun drawDynamicContent(
        canvas: Canvas,
        time: String,
        meridiem: String,
        date: String,
        weather: WeatherRepository.Reading,
    ) {
        drawClock(canvas, time, meridiem)
        drawDate(canvas, date)
        drawWeather(canvas, weather)
    }

    private fun drawClock(canvas: Canvas, time: String, meridiem: String) {
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(202, 109, 136)
            textSize = designWidth * 0.117f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            letterSpacing = 0.01f
            setShadowLayer(2.5f, 0f, 1f, 0x55FFFFFF)
        }
        val meridiemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(202, 109, 136)
            textSize = designWidth * 0.0305f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val timeWidth = timePaint.measureText(time)
        val meridiemWidth = meridiemPaint.measureText(meridiem)
        val groupWidth = timeWidth + designWidth * 0.006f + meridiemWidth
        val clockLeft = designWidth * 0.235f
        val clockWidth = designWidth * 0.445f
        val timeX = clockLeft + (clockWidth - groupWidth) / 2f
        val clockBottom = designHeight * 0.54f

        canvas.drawText(time, timeX, clockBottom - 5f, timePaint)
        canvas.drawText(
            meridiem,
            timeX + timeWidth + designWidth * 0.006f,
            clockBottom - designWidth * 0.016f,
            meridiemPaint,
        )
    }

    private fun drawDate(canvas: Canvas, date: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(178, 105, 126)
            textSize = designWidth * 0.0225f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val iconSize = designWidth * 0.0155f
        val gap = designWidth * 0.013f
        val textWidth = paint.measureText(date)
        val totalWidth = iconSize + gap + textWidth
        val centerX = designWidth * (0.306f + 0.333f / 2f)
        val centerY = designHeight * 0.758f +
            designHeight * 0.106f / 2f -
            designWidth * 0.0045f
        val iconLeft = centerX - totalWidth / 2f
        val iconTop = centerY - iconSize / 2f
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(178, 105, 126)
            style = Paint.Style.STROKE
            strokeWidth = designWidth * 0.002f
        }
        canvas.drawRoundRect(
            iconLeft,
            iconTop,
            iconLeft + iconSize,
            iconTop + iconSize,
            designWidth * 0.0028f,
            designWidth * 0.0028f,
            iconPaint,
        )
        canvas.drawLine(
            iconLeft,
            iconTop + iconSize * 0.29f,
            iconLeft + iconSize,
            iconTop + iconSize * 0.29f,
            iconPaint,
        )
        canvas.drawText(
            date,
            iconLeft + iconSize + gap,
            centerY - centeredBaselineOffset(paint),
            paint,
        )
    }

    private fun drawWeather(canvas: Canvas, weather: WeatherRepository.Reading) {
        val temperaturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(62, 65, 71)
            textSize = designWidth * 0.055f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val conditionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(62, 65, 71)
            textSize = designWidth * 0.0225f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val x = designWidth * 0.828f
        canvas.drawText(weather.temperature, x, designHeight * 0.80f, temperaturePaint)
        canvas.drawText(weather.condition, x, designHeight * 0.874f, conditionPaint)
    }
}