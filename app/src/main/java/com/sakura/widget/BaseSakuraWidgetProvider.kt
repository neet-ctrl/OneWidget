package com.sakura.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.widget.RemoteViews
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

abstract class BaseSakuraWidgetProvider : AppWidgetProvider() {
    protected abstract val artworkResId: Int
    protected abstract val layoutResId: Int
    protected abstract val designWidth: Float
    protected abstract val designHeight: Float

    protected abstract fun drawDynamicContent(
        canvas: Canvas,
        time: String,
        meridiem: String,
        date: String,
        weather: WeatherRepository.Reading,
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        scheduleRefresh(context)
        val result = goAsync()
        updateWidgets(context, appWidgetIds) {
            result.finish()
        }
    }

    override fun onEnabled(context: Context) {
        scheduleRefresh(context)
        val result = goAsync()
        updateWidgets(context) {
            result.finish()
        }
    }

    override fun onDisabled(context: Context) {
        cancelRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            ACTION_EXACT_ALARM_PERMISSION_CHANGED,
            -> {
                scheduleRefresh(context)
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
            ComponentName(context, javaClass),
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
        // One India-time snapshot keeps time and date consistent around a minute boundary.
        val now = ZonedDateTime.now(INDIA_ZONE)
        val time = TIME_FORMATTER.format(now)
        val meridiem = MERIDIEM_FORMATTER.format(now)
        val date = DATE_FORMATTER.format(now)

        ids.forEach { id ->
            val bitmap = renderBitmap(
                context,
                manager,
                id,
                time,
                meridiem,
                date,
                weather,
            )
            val views = RemoteViews(context.packageName, layoutResId).apply {
                setImageViewBitmap(R.id.widget_render, bitmap)
                setOnClickPendingIntent(
                    R.id.widget_time_touch,
                    pickerPendingIntent(context, AppPickerActivity.TARGET_TIME, REQUEST_TIME),
                )
                setOnClickPendingIntent(
                    R.id.widget_date_touch,
                    pickerPendingIntent(context, AppPickerActivity.TARGET_DATE, REQUEST_DATE),
                )
                setOnClickPendingIntent(
                    R.id.widget_weather_touch,
                    pickerPendingIntent(context, AppPickerActivity.TARGET_WEATHER, REQUEST_WEATHER),
                )
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

    private fun pickerPendingIntent(
        context: Context,
        target: String,
        requestCode: Int,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, AppPickerActivity::class.java).putExtra(
                AppPickerActivity.EXTRA_TARGET,
                target,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

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
        val source = BitmapFactory.decodeResource(context.resources, artworkResId)
        canvas.drawBitmap(
            source,
            null,
            Rect(0, 0, widthPx, heightPx),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        canvas.scale(widthPx / designWidth, heightPx / designHeight)
        drawDynamicContent(canvas, time, meridiem, date, weather)
        source.recycle()
        return bitmap
    }

    protected fun centeredBaselineOffset(paint: Paint): Float {
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

    private fun scheduleRefresh(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = refreshPendingIntent(context)
        val nextSecond = ZonedDateTime.now(INDIA_ZONE)
            .withNano(0)
            .plusSeconds(1)
            .toInstant()
            .toEpochMilli()

        alarmManager.cancel(pendingIntent)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextSecond,
                    pendingIntent,
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextSecond,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextSecond,
                pendingIntent,
            )
        }
    }

    private fun cancelRefresh(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(refreshPendingIntent(context))
    }

    private fun refreshPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            refreshRequestCode,
            Intent(context, javaClass).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private val refreshRequestCode: Int
        get() = if (this is SecondSakuraWidgetProvider) 1002 else 1001

    companion object {
        private const val ACTION_REFRESH = "com.sakura.widget.ACTION_REFRESH"
        private const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        private const val REQUEST_TIME = 2001
        private const val REQUEST_DATE = 2002
        private const val REQUEST_WEATHER = 2003
        private const val PREFERENCES = "sakura_widget_preferences"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_CONDITION = "condition"
        private const val KEY_WEATHER_TIME = "weather_time"
        private const val KEY_WEATHER_ATTEMPT = "weather_attempt"
        private const val WEATHER_REFRESH_MS = 15 * 60 * 1000L
        private val INDIA_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm", Locale.ENGLISH)
        private val MERIDIEM_FORMATTER = DateTimeFormatter.ofPattern("a", Locale.ENGLISH)
        private val DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
        private val refreshLock = Any()
        private var weatherFetchInProgress = false
    }
}