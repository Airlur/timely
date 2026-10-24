package com.airlur.timely

import com.airlur.timely.domain.CardDisplay
import com.airlur.timely.domain.DateMath
import com.airlur.timely.domain.Prefix
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateMathTest {

    private val today = LocalDate.of(2026, 10, 24)

    @Test
    fun `day diff basic`() {
        assertEquals(66, DateMath.dayDiff(LocalDate.of(2026, 12, 29), today))
        assertEquals(-20, DateMath.dayDiff(LocalDate.of(2026, 10, 4), today))
    }

    @Test
    fun `card display future past today ordinal`() {
        assertEquals(
            CardDisplay(Prefix.FUTURE, 66),
            DateMath.cardDisplay(LocalDate.of(2026, 12, 29), today, false)
        )
        assertEquals(
            CardDisplay(Prefix.PAST, 20),
            DateMath.cardDisplay(LocalDate.of(2026, 10, 4), today, false)
        )
        assertEquals(
            CardDisplay(Prefix.TODAY, 0),
            DateMath.cardDisplay(today, today, false)
        )
        // 含首日：今天 → 第 1 天；过去 20 天 → 第 21 天
        assertEquals(
            CardDisplay(Prefix.ORDINAL, 1),
            DateMath.cardDisplay(today, today, true)
        )
        assertEquals(
            CardDisplay(Prefix.ORDINAL, 21),
            DateMath.cardDisplay(LocalDate.of(2026, 10, 4), today, true)
        )
    }

    @Test
    fun `ymd composition across month and year`() {
        // 2025-06-01 → 2026-10-24：1 年 4 个月 23 天
        val (y, m, d) = DateMath.ymd(LocalDate.of(2025, 6, 1), today)
        assertEquals(1, y); assertEquals(4, m); assertEquals(23, d)
        // 闰年 2 月：2024-02-29 → 2025-02-28 记为整 1 年（特判口径）
        val t2 = DateMath.ymd(LocalDate.of(2024, 2, 29), LocalDate.of(2025, 2, 28))
        assertEquals(1, t2.first); assertEquals(0, t2.second); assertEquals(0, t2.third)
    }

    @Test
    fun `detail numbers include days weeks years`() {
        val nums = DateMath.detailNumbers(LocalDate.of(2026, 12, 29), today, false)
        assertEquals("66", nums.first().value)
        assertTrue(nums.any { it.unitKey == "week+3" })       // 9 周 3 天
        assertTrue(nums.any { it.unitKey == "month+5" })      // 2 个月 5 天
    }

    private fun assertTrue(b: Boolean) = org.junit.Assert.assertTrue(b)
}
