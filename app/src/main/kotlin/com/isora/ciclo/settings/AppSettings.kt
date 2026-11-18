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

    companion object {
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
