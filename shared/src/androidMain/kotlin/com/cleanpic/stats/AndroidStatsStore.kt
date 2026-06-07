package com.cleanpic.stats

import android.content.Context
import com.cleanpic.model.MediaType
import com.cleanpic.model.StatsSnapshot

/** 写入现有 cleanpic_prefs，新 key，紧凑编码；不动任何老数据。 */
class AndroidStatsStore(context: Context) : StatsStore {
    private val prefs = context.getSharedPreferences("cleanpic_prefs", Context.MODE_PRIVATE)
    private val key = "stats_snapshot_v1"

    override fun load(): StatsSnapshot = StatsCodec.decode(prefs.getString(key, null))

    override fun recordRoundReached(type: MediaType, nowMillis: Long, today: String) =
        save(StatsAggregator.applyRound(load(), type, nowMillis, today))

    override fun recordDeletion(type: MediaType, nowMillis: Long, today: String, bytes: Long, count: Int) =
        save(StatsAggregator.applyDeletion(load(), type, nowMillis, today, bytes, count))

    override fun reset() { prefs.edit().remove(key).apply() }

    private fun save(s: StatsSnapshot) {
        prefs.edit().putString(key, StatsCodec.encode(s)).apply()
    }
}
