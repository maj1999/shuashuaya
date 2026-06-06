package com.cleanpic.media

import com.cleanpic.model.MediaItem

/**
 * 随机选取的持久化浏览记忆（按 MediaType 各存一份）。
 *
 * - [cycle]：洗牌袋当前循环号。袋中非保留项耗尽则 cycle++ 开新循环。
 * - [records]：每个媒体 id 的浏览记录。
 *
 * 详见 docs/architecture/cleanpic/random-picker.md
 */
data class PickState(
    val cycle: Int = 0,
    val records: Map<String, SeenRecord> = emptyMap(),
)

/**
 * 单个媒体的浏览记录。
 *
 * - [lastDrawnCycle]：上次被抽中时的循环号，用于实现"本循环不重复"的洗牌袋语义。
 * - [lastSeenMillis]：上次出现时间（epoch 毫秒），用于天数新鲜度与"最久优先"补位。
 * - [kept]：是否被用户保留过；保留过的沉底，仅在非保留项耗尽时才再出现。
 */
data class SeenRecord(
    val lastDrawnCycle: Int,
    val lastSeenMillis: Long,
    val kept: Boolean,
)

/** 抽取结果：本轮选出的媒体 + 更新后的浏览记忆。 */
data class PickResult(
    val items: List<MediaItem>,
    val state: PickState,
)

/**
 * PickState 的零依赖紧凑编解码（不引入 kotlinx-serialization 运行时）。
 *
 * 格式：`cycle <HEADER> record <RECORD> record ...`
 * 每条 record：`id <FIELD> lastDrawnCycle <FIELD> lastSeenMillis <FIELD> kept(0/1)`
 * 使用控制字符 U+0001/U+0002/U+0003 作分隔符，媒体 id 不会包含这些字符。
 */
object PickStateCodec {
    private const val FIELD = '\u0001'
    private const val RECORD = '\u0002'
    private const val HEADER = '\u0003'

    fun encode(state: PickState): String {
        val body = state.records.entries.joinToString(RECORD.toString()) { (id, r) ->
            buildString {
                append(id); append(FIELD)
                append(r.lastDrawnCycle); append(FIELD)
                append(r.lastSeenMillis); append(FIELD)
                append(if (r.kept) '1' else '0')
            }
        }
        return "${state.cycle}$HEADER$body"
    }

    fun decode(raw: String?): PickState {
        if (raw.isNullOrEmpty()) return PickState()
        val headerIdx = raw.indexOf(HEADER)
        if (headerIdx < 0) return PickState()
        val cycle = raw.substring(0, headerIdx).toIntOrNull() ?: 0
        val body = raw.substring(headerIdx + 1)
        if (body.isEmpty()) return PickState(cycle, emptyMap())
        val records = HashMap<String, SeenRecord>()
        for (chunk in body.split(RECORD)) {
            if (chunk.isEmpty()) continue
            val parts = chunk.split(FIELD)
            if (parts.size != 4) continue
            val id = parts[0]
            val drawn = parts[1].toIntOrNull() ?: continue
            val seen = parts[2].toLongOrNull() ?: continue
            val kept = parts[3] == "1"
            records[id] = SeenRecord(drawn, seen, kept)
        }
        return PickState(cycle, records)
    }
}
