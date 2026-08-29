package com.sakura.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        if (intent.action == ACTION_REFRESH) {
            val result = goAsync()
            updateWidgets(context) {
                result.finish()
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
        val weatherIsStale = System.currentTimeMillis() -
            preferences.getLong(KEY_WEATHER_TIME, 0L) > WEATHER_REFRESH_MS

        if (!weatherIsStale) {
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
        val now = Date()
        val time = SimpleDateFormat("hh:mm", Locale.getDefault()).format(now)
        val meridiem = SimpleDateFormat("a", Locale.getDefault()).format(now)
        val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)

        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_sakura).apply {
                setTextViewText(R.id.widget_time, time)
                setTextViewText(R.id.widget_meridiem, meridiem)
                setTextViewText(R.id.widget_date, date)
                setTextViewText(R.id.widget_temperature, weather.temperature)
                setTextViewText(R.id.widget_condition, weather.condition)
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

    private fun loadWeather(context: Context): WeatherRepository.Reading {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        return WeatherRepository.Reading(
            temperature = preferences.getString(KEY_TEMPERATURE, "34°") ?: "34°",
            condition = preferences.getString(KEY_CONDITION, "Cloudy") ?: "Cloudy",
        )
    }

    private fun scheduleMinuteRefresh(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + 60_000L,
            60_000L,
            refreshPendingIntent(context),
        )
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
        private const val ACTION_REFRESH = "com.sakura.widget.ACTION_REFRESH"
        private const val PREFERENCES = "sakura_widget_preferences"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_CONDITION = "condition"
        private const val KEY_WEATHER_TIME = "weather_time"
        private const val WEATHER_REFRESH_MS = 15 * 60 * 1000L
    }
}