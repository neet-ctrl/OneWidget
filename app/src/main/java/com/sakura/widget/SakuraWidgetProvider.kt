package com.sakura.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.widget.RemoteViews
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class SakuraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        scheduleMinuteRefresh(context)
        val result = goAsync()
        updateWidgets(context, appWidgetIds) {
            result.finish()
        }
    }

    override fun onEnabled(context: Context) {
        scheduleMinuteRefresh(context)
        val result = goAsync()
        updateWidgets(context) {
            result.finish()
        }
    }

    override fun onDisabled(context: Context) {
        cancelMinuteRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED
            -> {
                scheduleMinuteRefresh(context)
                val result = goAsync()
                updateWidgets(context) {
                    result.finish()
                }
            }
        }
    }

    private fun updateWidgets(
        context: Context,
        appWidgetIds: IntArray? = null,
        onFinished: () -> Unit = {},
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = appWidgetIds ?: manager.getAppWidgetIds(
            ComponentName(context, SakuraWidgetProvider::class.java),
        )
        if (ids.isEmpty()) {
            onFinished()
            return
        }

        render(context, manager, ids, loadWeather(context))

        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val shouldFetchWeather = synchronized(refreshLock) {
            val weatherIsStale = now -
                preferences.getLong(KEY_WEATHER_ATTEMPT, 0L) >= WEATHER_REFRESH_MS

            if (!weatherIsStale || weatherFetchInProgress) {
                false
            } else {
                weatherFetchInProgress = true
                preferences.edit().putLong(KEY_WEATHER_ATTEMPT, now).apply()
                true
            }
        }

        if (!shouldFetchWeather) {
            onFinished()
            return
        }

        Thread {
            try {
                WeatherRepository.fetch()?.let { reading ->
                    preferences.edit()
                        .putString(KEY_TEMPERATURE, reading.temperature)
                        .putString(KEY_CONDITION, reading.condition)
                        .putLong(KEY_WEATHER_TIME, System.currentTimeMillis())
                        .apply()

                    render(context, manager, ids, reading)
                }
            } finally {
                synchronized(refreshLock) {
                    weatherFetchInProgress = false
                }
                onFinished()
            }
        }.start()
    }

    private fun render(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        weather: WeatherRepository.Reading,
    ) {
        // Take one timezone-aware snapshot so the displayed clock and date
        // can never come from different instants around a minute or midnight.
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val time = TIME_FORMATTER.format(now)
        val meridiem = MERIDIEM_FORMATTER.format(now)
        val date = DATE_FORMATTER.format(now)

        ids.forEach { id ->
            val bitmap = renderBitmap(
                context = context,
                manager = manager,
                appWidgetId = id,
                time = time,
                meridiem = meridiem,
                date = date,
                weather = weather,
            )
            val views = RemoteViews(context.packageName, R.layout.widget_sakura).apply {
                setImageViewBitmap(R.id.widget_render, bitmap)
                setContentDescription(
                    R.id.widget_root,
                    context.getString(
                        R.string.widget_content_description,
                        time,
                        date,
                        weather.temperature,
                        weather.condition,
                    ),
                )
            }
            manager.updateAppWidget(id, views)
        }
    }

    private fun renderBitmap(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        time: String,
        meridiem: String,
        date: String,
        weather: WeatherRepository.Reading,
    ): Bitmap {
        val options = manager.getAppWidgetOptions(appWidgetId)
        val widthDp = max(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
        ).coerceAtLeast(250)
        val heightDp = max(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
        ).coerceAtLeast(110)
        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).roundToInt().coerceAtLeast(1)
        val heightPx = (heightDp * density).roundToInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val source = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.widget_art_clean,
        )
        canvas.drawBitmap(
            source,
            null,
            Rect(0, 0, widthPx, heightPx),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )

        canvas.scale(widthPx / DESIGN_WIDTH, heightPx / DESIGN_HEIGHT)
        drawClock(canvas, time, meridiem)
        drawDate(canvas, date)
        drawWeather(canvas, weather)
        source.recycle()
        return bitmap
    }

    private fun drawClock(canvas: Canvas, time: String, meridiem: String) {
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(202, 109, 136)
            textSize = DESIGN_WIDTH * 0.117f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            letterSpacing = 0.01f
            setShadowLayer(2.5f, 0f, 1f, 0x55FFFFFF)
        }
        val meridiemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(202, 109, 136)
            textSize = DESIGN_WIDTH * 0.0305f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val timeWidth = timePaint.measureText(time)
        val meridiemWidth = meridiemPaint.measureText(meridiem)
        val groupWidth = timeWidth + DESIGN_WIDTH * 0.006f + meridiemWidth
        val clockLeft = DESIGN_WIDTH * 0.235f
        val clockWidth = DESIGN_WIDTH * 0.445f
        val timeX = clockLeft + (clockWidth - groupWidth) / 2f
        val clockBottom = DESIGN_HEIGHT * 0.54f

        canvas.drawText(time, timeX, clockBottom - 5f, timePaint)
        canvas.drawText(
            meridiem,
            timeX + timeWidth + DESIGN_WIDTH * 0.006f,
            clockBottom - DESIGN_WIDTH * 0.016f,
            meridiemPaint,
        )
    }

    private fun drawDate(canvas: Canvas, date: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(178, 105, 126)
            textSize = DESIGN_WIDTH * 0.0225f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val iconSize = DESIGN_WIDTH * 0.0155f
        val gap = DESIGN_WIDTH * 0.013f
        val textWidth = paint.measureText(date)
        val totalWidth = iconSize + gap + textWidth
        val centerX = DESIGN_WIDTH * (0.306f + 0.333f / 2f)
        val centerY = DESIGN_HEIGHT * 0.758f +
            DESIGN_HEIGHT * 0.106f / 2f -
            DESIGN_WIDTH * 0.0045f
        val iconLeft = centerX - totalWidth / 2f
        val iconTop = centerY - iconSize / 2f
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(178, 105, 126)
            style = Paint.Style.STROKE
            strokeWidth = DESIGN_WIDTH * 0.002f
        }
        canvas.drawRoundRect(
            iconLeft,
            iconTop,
            iconLeft + iconSize,
            iconTop + iconSize,
            DESIGN_WIDTH * 0.0028f,
            DESIGN_WIDTH * 0.0028f,
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
            textSize = DESIGN_WIDTH * 0.055f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val conditionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(62, 65, 71)
            textSize = DESIGN_WIDTH * 0.0225f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val x = DESIGN_WIDTH * 0.828f
        canvas.drawText(weather.temperature, x, DESIGN_HEIGHT * 0.80f, temperaturePaint)
        canvas.drawText(weather.condition, x, DESIGN_HEIGHT * 0.874f, conditionPaint)
    }

    private fun centeredBaselineOffset(paint: Paint): Float {
        val metrics = paint.fontMetrics
        return (metrics.ascent + metrics.descent) / 2f
    }

    private fun loadWeather(context: Context): WeatherRepository.Reading {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        return WeatherRepository.Reading(
            temperature = preferences.getString(KEY_TEMPERATURE, "34°") ?: "34°",
            condition = preferences.getString(KEY_CONDITION, "Cloudy") ?: "Cloudy",
        )
    }

    private fun scheduleMinuteRefresh(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = refreshPendingIntent(context)
        val nextMinute = ZonedDateTime.now(ZoneId.systemDefault())
            .withSecond(0)
            .withNano(0)
            .plusMinutes(1)
            .toInstant()
            .toEpochMilli()

        alarmManager.cancel(pendingIntent)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMinute,
                    pendingIntent,
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMinute,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            // The exact-alarm permission can change while the app is running.
            // Keep the clock alive with an idle-safe inexact alarm instead.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMinute,
                pendingIntent,
            )
        }
    }

    private fun cancelMinuteRefresh(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(refreshPendingIntent(context))
    }

    private fun refreshPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            1001,
            Intent(context, SakuraWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val DESIGN_WIDTH = 1280f
        private const val DESIGN_HEIGHT = 563f
        private const val ACTION_REFRESH = "com.sakura.widget.ACTION_REFRESH"
        private const val PREFERENCES = "sakura_widget_preferences"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_CONDITION = "condition"
        private const val KEY_WEATHER_TIME = "weather_time"
        private const val KEY_WEATHER_ATTEMPT = "weather_attempt"
        private const val WEATHER_REFRESH_MS = 15 * 60 * 1000L
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm", Locale.ENGLISH)
        private val MERIDIEM_FORMATTER = DateTimeFormatter.ofPattern("a", Locale.ENGLISH)
        private val DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
        private val refreshLock = Any()
        private var weatherFetchInProgress = false
    }
}