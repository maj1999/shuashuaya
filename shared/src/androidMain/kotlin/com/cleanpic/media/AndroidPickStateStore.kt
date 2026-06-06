package com.cleanpic.media

import android.content.Context
import android.content.SharedPreferences
import com.cleanpic.model.MediaType

/**
 * Android 浏览记忆持久化：复用 cleanpic_prefs，PickStateCodec 紧凑编码，零新依赖。
 */
class AndroidPickStateStore(context: Context) : PickStateStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cleanpic_prefs", Context.MODE_PRIVATE)

    private fun key(type: MediaType) = when (type) {
        MediaType.PHOTO -> "pick_state_photo"
        MediaType.VIDEO -> "pick_state_video"
    }

    override fun load(type: MediaType): PickState =
        PickStateCodec.decode(prefs.getString(key(type), null))

    override fun save(type: MediaType, state: PickState) {
        prefs.edit().putString(key(type), PickStateCodec.encode(state)).apply()
    }

    override fun clearAll() {
        prefs.edit()
            .remove(key(MediaType.PHOTO))
            .remove(key(MediaType.VIDEO))
            .apply()
    }
}
