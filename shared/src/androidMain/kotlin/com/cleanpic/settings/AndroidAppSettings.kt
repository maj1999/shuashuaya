package com.cleanpic.settings

import android.content.Context
import android.content.SharedPreferences

class AndroidAppSettings(context: Context) : AppSettings {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cleanpic_prefs", Context.MODE_PRIVATE)

    override var theme: String
        get() = prefs.getString("theme", "dreamy-gradient") ?: "dreamy-gradient"
        set(value) = prefs.edit().putString("theme", value).apply()

    override var interactionMode: String
        get() = prefs.getString("interaction_mode", "carousel") ?: "carousel"
        set(value) = prefs.edit().putString("interaction_mode", value).apply()

    private val validCounts = setOf(5, 10, 15, 20)

    override var roundCount: Int
        get() = prefs.getInt("round_count", 10).let {
            if (it in validCounts) it else 10
        }
        set(value) {
            if (value in validCounts) {
                prefs.edit().putInt("round_count", value).apply()
            }
        }
}
