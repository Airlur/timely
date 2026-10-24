package com.airlur.timely

import com.airlur.timely.data.Repeat
import com.airlur.timely.domain.RepeatRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RepeatRuleTest {

    private val base = LocalDate.of(2026, 1, 31) // 月末溢出样例
    private val after = LocalDate.of(2026, 3, 15)

    @Test
    fun `daily and weekly`() {
        // 含当天：after 本身就是出现日时返回 after（提醒不会跳过今天）
        assertEquals(LocalDate.of(2026, 3, 15), RepeatRule.nextOccurrence(Repeat.DAILY, base, after, null))
        assertEquals(LocalDate.of(2026, 3, 21), RepeatRule.nextOccurrence(Repeat.WEEKLY, base, after, null))
    }

    @Test
    fun `monthly clamps end of month`() {
        // 1 月 31 日每月重复，3 月应取 3 月 31 日
        assertEquals(LocalDate.of(2026, 3, 31), RepeatRule.nextOccurrence(Repeat.MONTHLY, base, after, null))
        // 2 月钳制到 28
        assertEquals(LocalDate.of(2026, 2, 28), RepeatRule.nextOccurrence(Repeat.MONTHLY, base, LocalDate.of(2026, 2, 10), null))
    }

    @Test
    fun `yearly keeps leap day behavior`() {
        assertEquals(LocalDate.of(2027, 1, 31), RepeatRule.nextOccurrence(Repeat.YEARLY, base, after, null))
    }

    @Test
    fun `custom every 2 weeks on mon wed fri`() {
        val rule = "2,WEEK,1,3,5"
        val from = LocalDate.of(2026, 10, 24) // 周六
        val next = RepeatRule.nextOccurrence(Repeat.CUSTOM, LocalDate.of(2026, 10, 1), from, rule)
        assertEquals(java.time.DayOfWeek.MONDAY, next!!.dayOfWeek)
        assertEquals(LocalDate.of(2026, 10, 26), next)
    }

    @Test
    fun `none returns null`() {
        assertNull(RepeatRule.nextOccurrence(Repeat.NONE, base, after, null))
    }
}
