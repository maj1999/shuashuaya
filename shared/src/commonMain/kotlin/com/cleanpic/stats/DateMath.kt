package com.cleanpic.stats

/**
 * 纯日期运算（commonMain 不可用 java.time）。
 * 把 "yyyy-MM-dd" 与「自 1970-01-01 起的天序号」互转，用于连续天数 / 月度聚合。
 * 采用 Howard Hinnant 的 days_from_civil 算法，覆盖闰年与世纪规则。
 */
object DateMath {

    /** "yyyy-MM-dd" → 自 1970-01-01 起的天序号（可负）；非法格式返回 null。 */
    fun epochDay(date: String): Int? {
        val parts = date.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (m < 1 || m > 12 || d < 1 || d > 31) return null
        val yy = if (m <= 2) y - 1 else y
        val era = (if (yy >= 0) yy else yy - 399) / 400
        val yoe = yy - era * 400
        val doy = (153 * (if (m > 2) m - 3 else m + 9) + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    /** "yyyy-MM-dd" 的「年-月」键，如 "2026-06"；非法返回 null。 */
    fun yearMonth(date: String): String? {
        val parts = date.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (m < 1 || m > 12) return null
        val mm = if (m < 10) "0$m" else "$m"
        return "$y-$mm"
    }
}
