package com.cleanpic.stats

import com.cleanpic.model.DailyStat

/**
 * 连续清理天数（streak）：纯函数，基于 daily 明细的日期集合计算。
 *
 * 口径：从「今天」起向前数连续有清理活动（daily 中存在该天条目）的天数。
 * 若今天还没清理但昨天清理过，则从昨天起算——避免「今天还没打开」就把 streak 清零。
 */
object StatsStreak {

    fun current(daily: List<DailyStat>, today: String): Int {
        val days = daily.mapNotNull { DateMath.epochDay(it.date) }.toHashSet()
        val t = DateMath.epochDay(today) ?: return 0
        var cursor = when {
            t in days -> t
            (t - 1) in days -> t - 1
            else -> return 0
        }
        var streak = 0
        while (cursor in days) {
            streak++
            cursor--
        }
        return streak
    }
}
