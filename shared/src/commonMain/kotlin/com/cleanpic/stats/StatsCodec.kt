package com.cleanpic.stats

import com.cleanpic.model.*

/**
 * StatsSnapshot 的零依赖紧凑编解码（仿 PickStateCodec，不引入 kotlinx-serialization）。
 *
 * 格式：`<lifetime> H <daily> R <daily> ...`
 *   lifetime: pBytes F pCount F pRounds F vBytes F vCount F vRounds F firstAt F lastAt
 *   daily:    date F pBytes F pCount F pRounds F vBytes F vCount F vRounds
 * 控制字符 U+0001/U+0002/U+0003 作分隔符，date 不含这些字符。
 */
object StatsCodec {
    private const val F = ''   // 字段
    private const val R = ''   // daily 记录
    private const val H = ''   // lifetime / daily 段分隔

    fun encode(s: StatsSnapshot): String {
        val l = s.lifetime
        val head = listOf(
            l.photo.bytes, l.photo.count, l.photo.rounds,
            l.video.bytes, l.video.count, l.video.rounds,
            l.firstCleanupAt, l.lastCleanupAt,
        ).joinToString(F.toString())
        val body = s.daily.joinToString(R.toString()) { d ->
            listOf(
                d.date, d.photo.bytes, d.photo.count, d.photo.rounds,
                d.video.bytes, d.video.count, d.video.rounds,
            ).joinToString(F.toString())
        }
        return "$head$H$body"
    }

    fun decode(raw: String?): StatsSnapshot {
        if (raw.isNullOrEmpty()) return StatsSnapshot()
        val h = raw.indexOf(H)
        if (h < 0) return StatsSnapshot()
        val head = raw.substring(0, h).split(F)
        if (head.size != 8) return StatsSnapshot()
        val lifetime = LifetimeStats(
            photo = MediaTypeStats(
                head[0].toLongOrNull() ?: 0L,
                head[1].toIntOrNull() ?: 0,
                head[2].toIntOrNull() ?: 0,
            ),
            video = MediaTypeStats(
                head[3].toLongOrNull() ?: 0L,
                head[4].toIntOrNull() ?: 0,
                head[5].toIntOrNull() ?: 0,
            ),
            firstCleanupAt = head[6].toLongOrNull() ?: 0L,
            lastCleanupAt = head[7].toLongOrNull() ?: 0L,
        )
        val body = raw.substring(h + 1)
        val daily = if (body.isEmpty()) emptyList() else body.split(R).mapNotNull { chunk ->
            if (chunk.isEmpty()) return@mapNotNull null
            val p = chunk.split(F)
            if (p.size != 7) return@mapNotNull null
            DailyStat(
                date = p[0],
                photo = MediaTypeStats(p[1].toLongOrNull() ?: 0L, p[2].toIntOrNull() ?: 0, p[3].toIntOrNull() ?: 0),
                video = MediaTypeStats(p[4].toLongOrNull() ?: 0L, p[5].toIntOrNull() ?: 0, p[6].toIntOrNull() ?: 0),
            )
        }
        return StatsSnapshot(lifetime, daily)
    }
}
