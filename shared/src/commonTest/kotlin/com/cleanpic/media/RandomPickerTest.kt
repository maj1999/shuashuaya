package com.cleanpic.media

import com.cleanpic.model.MediaItem
import com.cleanpic.model.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandomPickerTest {
    private fun makeItems(n: Int) = (1..n).map {
        MediaItem("id_$it", MediaType.PHOTO, "img_$it.jpg", 1000L, 0L, 100, 100)
    }

    @Test fun pick_normal_returns_correct_count() {
        val result = RandomPicker.pick(makeItems(100), 10, emptySet())
        assertEquals(10, result.size)
    }
    @Test fun pick_normal_no_duplicates() {
        val result = RandomPicker.pick(makeItems(100), 10, emptySet())
        assertEquals(result.size, result.map { it.id }.toSet().size)
    }
    @Test fun pick_insufficient_returns_all() {
        val result = RandomPicker.pick(makeItems(3), 10, emptySet())
        assertEquals(3, result.size)
    }
    @Test fun pick_empty_returns_empty() {
        val result = RandomPicker.pick(emptyList(), 10, emptySet())
        assertTrue(result.isEmpty())
    }
    @Test fun pick_excludes_shown_ids() {
        val items = makeItems(15)
        val exclude = items.take(10).map { it.id }.toSet()
        val result = RandomPicker.pick(items, 10, exclude)
        assertEquals(5, result.size)
        assertTrue(result.none { it.id in exclude })
    }
    @Test fun pick_all_excluded_resets() {
        val items = makeItems(10)
        val exclude = items.map { it.id }.toSet()
        val result = RandomPicker.pick(items, 10, exclude)
        assertEquals(10, result.size)
    }
    @Test fun pick_boundary_counts() {
        for (count in listOf(5, 10, 15, 20)) {
            val result = RandomPicker.pick(makeItems(100), count, emptySet())
            assertEquals(count, result.size)
        }
    }
}
