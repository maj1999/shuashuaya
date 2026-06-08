package com.cleanpic.stats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateMathTest {

    @Test fun epoch_origin_is_zero() {
        assertEquals(0, DateMath.epochDay("1970-01-01"))
    }

    @Test fun consecutive_days_differ_by_one() {
        val a = DateMath.epochDay("2026-06-07")!!
        val b = DateMath.epochDay("2026-06-08")!!
        assertEquals(1, b - a)
    }

    @Test fun spans_month_boundary() {
        val a = DateMath.epochDay("2026-06-30")!!
        val b = DateMath.epochDay("2026-07-01")!!
        assertEquals(1, b - a)
    }

    @Test fun leap_year_feb_29_is_valid_and_continuous() {
        val a = DateMath.epochDay("2024-02-28")!!
        val b = DateMath.epochDay("2024-02-29")!!
        val c = DateMath.epochDay("2024-03-01")!!
        assertEquals(1, b - a)
        assertEquals(1, c - b)
    }

    @Test fun garbage_returns_null() {
        assertNull(DateMath.epochDay("not-a-date"))
        assertNull(DateMath.epochDay("2026-13-01"))
        assertNull(DateMath.epochDay("2026-06"))
    }

    @Test fun year_month_key_pads_month() {
        assertEquals("2026-06", DateMath.yearMonth("2026-06-07"))
        assertEquals("2026-12", DateMath.yearMonth("2026-12-31"))
        assertNull(DateMath.yearMonth("bad"))
    }
}
