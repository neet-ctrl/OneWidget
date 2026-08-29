package com.sakura.widget

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.AlarmClock
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

/**
 * Routes widget taps to an installed app.
 *
 * A single unambiguous target opens directly. If Android reports several
 * matching apps, or none for the requested category, the user gets a
 * searchable launcher-app list and the choice is remembered for that area.
 */
class AppPickerActivity : Activity() {

    private lateinit var target: String
    private lateinit var appList: LinearLayout
    private lateinit var search: EditText
    private var allApps: List<AppOption> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = intent.getStringExtra(EXTRA_TARGET) ?: TARGET_TIME

        val preferredPackage = preferences.getString(preferenceKey(target), null)
        if (preferredPackage != null) {
            val preferred = resolveTargetApps(target)
                .firstOrNull { it.packageName == preferredPackage }
                ?: launcherApps().firstOrNull { it.packageName == preferredPackage }

            if (preferred != null && launch(preferred)) {
                return
            }
            preferences.edit().remove(preferenceKey(target)).apply()
        }

        val targetApps = resolveTargetApps(target)
        if (targetApps.size == 1 && launch(targetApps.first())) {
            return
        }

        showPicker()
    }

    private fun showPicker() {
        allApps = launcherApps()
        val rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(Color.rgb(255, 248, 250))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(16))
            background = rounded(Color.WHITE, 26f)
            elevation = dp(8).toFloat()
        }
        rootContainer.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        val heading = TextView(this).apply {
            text = getString(R.string.picker_title)
            setTextColor(Color.rgb(51, 42, 47))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        }
        header.addView(
            heading,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        val close = ImageButton(this).apply {
            contentDescription = getString(R.string.picker_close)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.rgb(198, 106, 134))
            background = rounded(Color.TRANSPARENT, 22f)
            setOnClickListener { finish() }
        }
        header.addView(close, LinearLayout.LayoutParams(dp(44), dp(44)))
        card.addView(header)

        val subtitle = TextView(this).apply {
            text = subtitleFor(target)
            setTextColor(Color.rgb(121, 105, 113))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(2), 0, dp(16))
        }
        card.addView(subtitle)

        search = EditText(this).apply {
            hint = getString(R.string.picker_search_hint)
            setSingleLine(true)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.rgb(51, 42, 47))
            setHintTextColor(Color.rgb(159, 143, 150))
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(Color.rgb(255, 244, 247), 18f)
        }
        card.addView(
            search,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52),
            ),
        )

        appList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            setPadding(0, dp(12), 0, 0)
            addView(appList)
        }
        card.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        setContentView(rootContainer)
        populateApps("")
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                populateApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun populateApps(query: String) {
        appList.removeAllViews()
        val normalizedQuery = query.trim().lowercase()
        val matches = allApps.filter {
            normalizedQuery.isEmpty() ||
                it.label.lowercase().contains(normalizedQuery)
        }

        if (matches.isEmpty()) {
            appList.addView(TextView(this).apply {
                text = getString(R.string.picker_no_apps)
                setTextColor(Color.rgb(121, 105, 113))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setPadding(dp(8), dp(20), dp(8), dp(20))
            })
            return
        }

        val savedPackage = preferences.getString(preferenceKey(target), null)
        matches.forEach { option ->
            appList.addView(appRow(option, option.packageName == savedPackage))
        }
    }

    private fun appRow(option: AppOption, selected: Boolean): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(
                if (selected) Color.rgb(255, 237, 242) else Color.WHITE,
                18f,
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (launch(option)) {
                    preferences.edit().putString(preferenceKey(target), option.packageName).apply()
                }
            }
        }

        val icon = ImageView(this).apply {
            setImageDrawable(option.icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(46), dp(46)))

        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(8), 0)
        }
        labels.addView(TextView(this).apply {
            text = option.label
            setTextColor(Color.rgb(51, 42, 47))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        labels.addView(TextView(this).apply {
            text = getString(R.string.picker_app_subtitle)
            setTextColor(Color.rgb(154, 136, 145))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        row.addView(
            labels,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )

        if (selected) {
            row.addView(TextView(this).apply {
                text = "✓"
                setTextColor(Color.rgb(198, 106, 134))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            }, LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(66),
        )
        params.bottomMargin = dp(8)
        row.layoutParams = params
        return row
    }

    private fun launch(option: AppOption): Boolean =
        try {
            startActivity(option.openIntent)
            finish()
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, option.label + " " + getString(R.string.picker_unavailable), Toast.LENGTH_SHORT)
                .show()
            false
        }

    private fun resolveTargetApps(target: String): List<AppOption> {
        val results = LinkedHashMap<String, AppOption>()
        targetIntents(target).forEach { queryIntent ->
            packageManager.queryIntentActivities(
                queryIntent,
                PackageManager.MATCH_DEFAULT_ONLY,
            ).forEach { info ->
                val option = appOption(info, queryIntent)
                results.putIfAbsent(option.packageName, option)
            }
        }
        return results.values.sortedBy { it.label.lowercase() }
    }

    private fun launcherApps(): List<AppOption> {
        val queryIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = LinkedHashMap<String, AppOption>()
        packageManager.queryIntentActivities(
            queryIntent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ).forEach { info ->
            val option = appOption(info, queryIntent)
            if (option.packageName != packageName) {
                results.putIfAbsent(option.packageName, option)
            }
        }
        return results.values.sortedBy { it.label.lowercase() }
    }

    private fun appOption(info: ResolveInfo, baseIntent: Intent): AppOption {
        val activityInfo = info.activityInfo
        return AppOption(
            packageName = activityInfo.packageName,
            label = info.loadLabel(packageManager).toString(),
            icon = info.loadIcon(packageManager),
            openIntent = Intent(baseIntent).setComponent(
                ComponentName(activityInfo.packageName, activityInfo.name),
            ),
        )
    }

    private fun targetIntents(target: String): List<Intent> =
        when (target) {
            TARGET_DATE -> listOf(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
            )
            TARGET_WEATHER -> listOf(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_WEATHER),
            )
            else -> listOf(
                Intent(AlarmClock.ACTION_SHOW_ALARMS),
            )
        }

    private fun subtitleFor(target: String): String =
        when (target) {
            TARGET_DATE -> getString(R.string.picker_date_subtitle)
            TARGET_WEATHER -> getString(R.string.picker_weather_subtitle)
            else -> getString(R.string.picker_time_subtitle)
        }

    private fun preferenceKey(target: String): String = "selected_app_$target"

    private val preferences
        get() = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private fun rounded(fillColor: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density).roundToInt()

    data class AppOption(
        val packageName: String,
        val label: String,
        val icon: Drawable,
        val openIntent: Intent,
    )

    companion object {
        const val EXTRA_TARGET = "com.sakura.widget.extra.TARGET"
        const val TARGET_TIME = "time"
        const val TARGET_DATE = "date"
        const val TARGET_WEATHER = "weather"
        private const val PREFERENCES = "sakura_widget_app_preferences"
    }
}