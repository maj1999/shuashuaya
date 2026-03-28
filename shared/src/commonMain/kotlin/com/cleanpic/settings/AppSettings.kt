package com.cleanpic.settings

interface AppSettings {
    var theme: String
    var interactionMode: String
    var roundCount: Int
}

private val VALID_COUNTS = setOf(5, 10, 15, 20)

class InMemoryAppSettings : AppSettings {
    override var theme: String = "dreamy-gradient"
    override var interactionMode: String = "carousel"

    private var _roundCount: Int = 10
    override var roundCount: Int
        get() = _roundCount
        set(value) {
            _roundCount = if (value in VALID_COUNTS) value else 10
        }
}
