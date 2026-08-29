package com.sakura.widget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Small no-key weather reader for the widget.
 *
 * Change the two coordinates below to the widget owner's city. The initial
 * values keep the supplied reference's Indian context and the widget still
 * shows 34° / Cloudy if the request is unavailable.
 */
object WeatherRepository {
    private const val LATITUDE = 28.6139
    private const val LONGITUDE = 77.2090

    data class Reading(
        val temperature: String,
        val condition: String,
    )

    fun fetch(): Reading? {
        val endpoint = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$LATITUDE&longitude=$LONGITUDE" +
            "&current=temperature_2m,weather_code&temperature_unit=celsius"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 7_000
            setRequestProperty("Accept", "application/json")
        }

        return try {
            if (connection.responseCode !in 200..299) return null
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(payload).getJSONObject("current")
            val temperature = current.getDouble("temperature_2m")
            val weatherCode = current.getInt("weather_code")

            Reading(
                temperature = String.format(Locale.US, "%.0f°", temperature),
                condition = conditionFor(weatherCode),
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun conditionFor(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly Cloudy"
        3, 45, 48 -> "Cloudy"
        in 51..57 -> "Drizzle"
        in 61..67, in 80..82 -> "Rainy"
        in 71..77, 85, 86 -> "Snowy"
        in 95..99 -> "Stormy"
        else -> "Cloudy"
    }
}