package com.airlur.timely.domain

import com.airlur.timely.data.Repeat
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object RepeatRule {

    /**
     * 事件在 after 之后的下一次出现日期（含当天）。
     * 自定义规则字符串格式："interval,UNIT,wd1,wd2..."（weekday = ISO 1..7）
     */
    fun nextOccurrence(repeat: Repeat, base: LocalDate, after: LocalDate, customRule: String?): LocalDate? {
        return when (repeat) {
            Repeat.NONE -> null
            Repeat.DAILY -> nextByDays(base, after, 1)
            Repeat.WEEKLY -> nextByDays(base, after, 7)
            Repeat.MONTHLY -> nextByMonths(base, after, 1)
            Repeat.YEARLY -> nextByYears(base, after, 1)
            Repeat.CUSTOM -> nextCustom(base, after, customRule)
        }
    }

    fun parseCustom(rule: String?): Triple<Int, String, Set<Int>>? {
        if (rule.isNullOrBlank()) return null
        val parts = rule.split(",")
        val interval = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val unit = parts.getOrNull(1) ?: "WEEK"
        val weekdays = parts.drop(2).mapNotNull { it.toIntOrNull() }.toSet()
        return Triple(interval, unit, weekdays)
    }

    private fun nextCustom(base: LocalDate, after: LocalDate, rule: String?): LocalDate? {
        val parsed = parseCustom(rule) ?: return null
        val (interval, unit, weekdays) = parsed
        if (unit == "WEEK" && weekdays.isNotEmpty()) {
            // 每隔 interval 周，落在指定星期
            var candidate = after
            for (i in 0 until 7 * (interval + 1) * 60) {
                candidate = candidate.plusDays(1)
                val weekAlign = weeksBetween(base, candidate) % interval == 0L
                if (weekAlign && candidate.dayOfWeek.value in weekdays) return candidate
            }
            return null
        }
        return when (unit) {
            "DAY" -> nextByDays(base, after, interval)
            "WEEK" -> nextByDays(base, after, interval * 7)
            "MONTH" -> nextByMonths(base, after, interval)
            "YEAR" -> nextByYears(base, after, interval)
            else -> null
        }
    }

    private fun weeksBetween(base: LocalDate, target: LocalDate): Long {
        val b = base.minusDays((base.dayOfWeek.value - 1).toLong())
        val t = target.minusDays((target.dayOfWeek.value - 1).toLong())
        return java.time.temporal.ChronoUnit.WEEKS.between(b, t)
    }

    private fun nextByDays(base: LocalDate, after: LocalDate, intervalDays: Int): LocalDate {
        if (!after.isAfter(base)) return base
        val diff = ChronoUnit.DAYS.between(base, after)
        val remainder = diff % intervalDays
        val add = if (remainder == 0L) diff else diff + (intervalDays.toLong() - remainder)
        return base.plusDays(add)
    }

    private fun nextByMonths(base: LocalDate, after: LocalDate, intervalMonths: Int): LocalDate {
        if (!after.isAfter(base)) return base
        val monthsDiff = (after.year - base.year) * 12L + (after.monthValue - base.monthValue)
        var candidate = addMonths(base, (monthsDiff / intervalMonths.toLong()) * intervalMonths)
        while (candidate.isBefore(after)) candidate = addMonths(candidate, intervalMonths)
        return candidate
    }

    private fun nextByYears(base: LocalDate, after: LocalDate, intervalYears: Int): LocalDate {
        if (!after.isAfter(base)) return base
        val yearsDiff = (after.year - base.year).toLong()
        var candidate = addYears(base, (yearsDiff / intervalYears.toLong()) * intervalYears)
        while (candidate.isBefore(after)) candidate = addYears(candidate, intervalYears)
        return candidate
    }

    /** 月末溢出钳制：1 月 31 日 → 2 月 28/29 日 */
    fun addMonths(date: LocalDate, months: Long): LocalDate {
        val y = date.year + (date.monthValue - 1 + months) / 12
        val m = ((date.monthValue - 1 + months) % 12 + 12) % 12 + 1
        val day = minOf(date.dayOfMonth, LocalDate.of(y.toInt(), m, 1).lengthOfMonth())
        return LocalDate.of(y.toInt(), m.toInt(), day)
    }

    fun addYears(date: LocalDate, years: Long): LocalDate =
        addMonths(date, years * 12)
}
