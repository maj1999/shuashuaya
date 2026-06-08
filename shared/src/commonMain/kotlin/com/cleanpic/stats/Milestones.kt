package com.cleanpic.stats

import com.cleanpic.model.LifetimeStats

/** 一枚里程碑徽章。 */
data class Badge(
    val id: String,
    val label: String,
    val icon: String,
    val achieved: Boolean,
)

/**
 * 里程碑徽章评估（纯函数）：按累计字节 / 文件数 / 轮次的阈值判定是否达成。
 * 全部返回（含未达成，UI 灰显），顺序固定，便于稳定渲染与测试。
 */
object Milestones {

    private const val GB = 1_000_000_000L

    /** (id, 文案, 图标, 维度取值, 阈值) */
    private val DEFS: List<Triple<String, String, Pair<String, Long>>> = listOf(
        Triple("bytes_1g", "清理 1 GB", "storage" to 1L * GB),
        Triple("bytes_10g", "清理 10 GB", "storage" to 10L * GB),
        Triple("bytes_100g", "清理 100 GB", "storage" to 100L * GB),
        Triple("count_100", "清理 100 个", "photo" to 100L),
        Triple("count_1000", "清理 1000 个", "photo" to 1000L),
        Triple("rounds_10", "完成 10 轮", "stats" to 10L),
        Triple("rounds_50", "完成 50 轮", "stats" to 50L),
        Triple("rounds_100", "完成 100 轮", "stats" to 100L),
    )

    fun evaluate(l: LifetimeStats): List<Badge> = DEFS.map { (id, label, dim) ->
        val (kind, threshold) = dim
        val value = when {
            id.startsWith("bytes_") -> l.totalBytes
            id.startsWith("count_") -> l.totalCount.toLong()
            else -> l.totalRounds.toLong()
        }
        Badge(id = id, label = label, icon = kind, achieved = value >= threshold)
    }

    /** 已达成数量（用于"X / Y"概览）。 */
    fun achievedCount(l: LifetimeStats): Int = evaluate(l).count { it.achieved }
}
