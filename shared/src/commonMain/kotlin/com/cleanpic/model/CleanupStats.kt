package com.cleanpic.model

/** 单一媒体类型（照片 或 视频）的统计三元组。 */
data class MediaTypeStats(
    val bytes: Long = 0L,
    val count: Int = 0,
    val rounds: Int = 0,
)

/** 累计聚合：照片 + 视频 各一份，所有"合计"由相加得出。 */
data class LifetimeStats(
    val photo: MediaTypeStats = MediaTypeStats(),
    val video: MediaTypeStats = MediaTypeStats(),
    val firstCleanupAt: Long = 0L,
    val lastCleanupAt: Long = 0L,
) {
    val totalBytes: Long get() = photo.bytes + video.bytes
    val totalCount: Int get() = photo.count + video.count
    val totalRounds: Int get() = photo.rounds + video.rounds
}

/** 按天明细（每天一条），照片/视频分别记。阶段一只采集，不展示。 */
data class DailyStat(
    val date: String,                 // "yyyy-MM-dd"（设备本地时区）
    val photo: MediaTypeStats = MediaTypeStats(),
    val video: MediaTypeStats = MediaTypeStats(),
)

/** 持久化的整体快照。 */
data class StatsSnapshot(
    val lifetime: LifetimeStats = LifetimeStats(),
    val daily: List<DailyStat> = emptyList(),
)

/** 设备存储现状（字节）。 */
data class StorageInfo(
    val totalBytes: Long,
    val availableBytes: Long,
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
}
