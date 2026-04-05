package com.cleanpic.settings

import platform.Foundation.NSUserDefaults

class IosAppSettings : AppSettings {
    private val defaults = NSUserDefaults.standardUserDefaults

    override var theme: String
        get() = defaults.stringForKey("theme") ?: "dreamy-gradient"
        set(value) {
            defaults.setObject(value, forKey = "theme")
        }

    override var interactionMode: String
        get() = defaults.stringForKey("interaction_mode") ?: "carousel"
        set(value) {
            defaults.setObject(value, forKey = "interaction_mode")
        }

    private val validCounts = setOf(5, 10, 15, 20)

    override var roundCount: Int
        get() = defaults.integerForKey("round_count").toInt().let {
            if (it in validCounts && it > 0) it else 10
        }
        set(value) {
            if (value in validCounts) {
                defaults.setInteger(value.toLong(), forKey = "round_count")
            }
        }

    override var autoCheckUpdate: Boolean
        get() {
            // NSUserDefaults returns false for unset booleans, so use object check for default=true
            val obj = defaults.objectForKey("auto_check_update")
            return if (obj == null) true else defaults.boolForKey("auto_check_update")
        }
        set(value) {
            defaults.setBool(value, forKey = "auto_check_update")
        }
}
