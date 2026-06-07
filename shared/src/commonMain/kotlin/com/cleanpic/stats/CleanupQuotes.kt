package com.cleanpic.stats

import com.cleanpic.model.LifetimeStats
import kotlin.math.abs

/** 治愈温柔语录池 + 情境化选句（纯函数，可测）。 */
object CleanupQuotes {

    val FIRST = listOf(
        "第一次清理完成，相册轻盈了一点点。",
        "万事开头难，你已经迈出第一步。",
        "清爽，从这一次开始。",
    )

    val STREAK = listOf(
        "又见面了，坚持整理的样子真好。",
        "保持节奏，相册会一直清清爽爽。",
        "今天也来收拾啦，给你点个赞。",
    )

    val DAILY = listOf(
        "干净的相册，像刚收拾好的房间。",
        "每一次清理，都是对自己温柔一点。",
        "少一点冗余，多一点清爽。",
        "整理好的不只是手机，还有心情。",
        "腾出的不只是空间，是翻相册的好心情。",
    )

    /**
     * @param isStreak 最近一次清理是否在"今天"（连续/活跃）。
     * @param seed 选句种子（调用方传，如累计轮次），保证纯函数可测、同种子稳定。
     */
    fun pick(stats: LifetimeStats, isStreak: Boolean, seed: Int): String {
        val pool = when {
            stats.totalRounds <= 1 -> FIRST
            isStreak -> STREAK
            else -> DAILY
        }
        return pool[abs(seed) % pool.size]
    }
}
