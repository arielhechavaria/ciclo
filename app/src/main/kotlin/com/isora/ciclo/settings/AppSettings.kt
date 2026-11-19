package com.isora.ciclo.settings

import android.content.Context
import java.time.LocalTime

/** Small preferences: the Friday notification switch and its time of day. */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ciclo_settings", Context.MODE_PRIVATE)

    var fridayEnabled: Boolean
        get() = prefs.getBoolean("friday_enabled", true)
        set(value) {
            prefs.edit().putBoolean("friday_enabled", value).apply()
        }

    var fridayTime: LocalTime
        get() = LocalTime.of(prefs.getInt("friday_hour", 8), prefs.getInt("friday_minute", 0))
        set(value) {
            prefs.edit().putInt("friday_hour", value.hour).putInt("friday_minute", value.minute).apply()
        }

    /** Months after the end of a treatment when the follow-up review is due. */
    var followUpMonths: Int
        get() = prefs.getInt("follow_up_months", DEFAULT_FOLLOW_UP_MONTHS).coerceIn(FOLLOW_UP_MONTHS_RANGE)
        set(value) {
            prefs.edit().putInt("follow_up_months", value.coerceIn(FOLLOW_UP_MONTHS_RANGE)).apply()
        }

    companion object {
        const val DEFAULT_FOLLOW_UP_MONTHS = 3
        val FOLLOW_UP_MONTHS_RANGE = 1..24

        /** Parses "H:mm" or "HH:mm"; null when it is not a valid time of day. */
        fun parseTime(text: String): LocalTime? {
            val parts = text.trim().split(':')
            if (parts.size != 2) return null
            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return LocalTime.of(hour, minute)
        }
    }
}
