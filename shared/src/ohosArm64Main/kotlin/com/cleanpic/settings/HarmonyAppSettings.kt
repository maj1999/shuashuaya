package com.cleanpic.settings

class HarmonyAppSettings : AppSettings {
    // TODO: 通过 OHOS Preferences API 实现持久化

    private var _theme = "dreamy-gradient"
    private var _mode = "carousel"
    private var _count = 10
    private val validCounts = setOf(5, 10, 15, 20)

    override var theme: String
        get() = _theme
        set(value) { _theme = value }

    override var interactionMode: String
        get() = _mode
        set(value) { _mode = value }

    override var roundCount: Int
        get() = _count
        set(value) {
            if (value in validCounts) _count = value
        }
}
