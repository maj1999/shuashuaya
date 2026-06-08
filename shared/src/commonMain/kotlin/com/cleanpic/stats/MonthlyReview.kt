package com.cleanpic.stats

import com.cleanpic.model.DailyStat
import com.cleanpic.model.MediaTypeStats

/** 某个自然月的清理汇总。 */
data class MonthStat(
    val yearMonth: String,            // "yyyy-MM"
    val photo: MediaTypeStats,
    val video: MediaTypeStats,
) {
    val totalBytes: Long get() = photo.bytes + video.bytes
    val totalCount: Int get() = photo.count + video.count
    val totalRounds: Int get() = photo.rounds + video.rounds
}

/**
 * 月度回顾（阶段三）：把 daily 明细按自然月聚合（纯函数）。
 * 数据底子来自阶段一就开始采集的 daily，故历史可回溯。
 */
object MonthlyReview {

    private fun MediaTypeStats.plus(o: MediaTypeStats) =
        MediaTypeStats(bytes = bytes + o.bytes, count = count + o.count, rounds = rounds + o.rounds)

    /** 按月聚合，结果按月份倒序（最近的月在前）。 */
    fun byMonth(daily: List<DailyStat>): List<MonthStat> {
        val acc = LinkedHashMap<String, MonthStat>()
        for (d in daily) {
            val ym = DateMath.yearMonth(d.date) ?: continue
            val cur = acc[ym]
            acc[ym] = if (cur == null) {
                MonthStat(ym, d.photo, d.video)
            } else {
                MonthStat(ym, cur.photo.plus(d.photo), cur.video.plus(d.video))
            }
        }
        return acc.values.sortedByDescending { it.yearMonth }
    }

    /** 指定月份的汇总；无数据返回 null。 */
    fun forMonth(daily: List<DailyStat>, yearMonth: String): MonthStat? =
        byMonth(daily).firstOrNull { it.yearMonth == yearMonth }
}
