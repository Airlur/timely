package com.airlur.timely.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 卡片右下角的前缀类型，文案由 UI 层按语言渲染 */
enum class Prefix { FUTURE, PAST, TODAY, ORDINAL }

data class CardDisplay(val prefix: Prefix, val number: Int, val ordinal: Boolean = false)

/** 详情页大数字的候选项：天 / 年月组合 / 总月数 / 周 */
data class DetailNumber(val value: String, val unitKey: String)

object DateMath {

    fun dayDiff(target: LocalDate, today: LocalDate): Int =
        ChronoUnit.DAYS.between(today, target).toInt()

    /** 列表卡片数字：还有 66 / 已经 875 / 就是今天 / 含首日的「第 N 天」 */
    fun cardDisplay(target: LocalDate, today: LocalDate, includeStartDay: Boolean): CardDisplay {
        val dd = dayDiff(target, today)
        return when {
            dd == 0 && includeStartDay -> CardDisplay(Prefix.ORDINAL, 1)
            dd == 0 -> CardDisplay(Prefix.TODAY, 0)
            includeStartDay && dd < 0 -> CardDisplay(Prefix.ORDINAL, -dd + 1)
            dd > 0 -> CardDisplay(Prefix.FUTURE, dd)
            else -> CardDisplay(Prefix.PAST, -dd)
        }
    }

    /** 年月日拆分：绝对值口径，与原型 ymd() 一致 */
    fun ymd(target: LocalDate, today: LocalDate): Triple<Int, Int, Int> {
        val after = !target.isBefore(today)
        val start = if (after) today else target
        val end = if (after) target else today
        var y = end.year - start.year
        var m = end.monthValue - start.monthValue
        var d = end.dayOfMonth - start.dayOfMonth
        if (d < 0) {
            m--
            d += start.lengthOfMonth()
        }
        if (m < 0) {
            y--
            m += 12
        }
        // 同月同日的整年（如闰 2/29 → 平年 2/28）按 1 年 0 月 0 天计
        if (m == 11 && d >= start.lengthOfMonth() - 1 && start.dayOfMonth >= 28 && end.dayOfMonth <= start.dayOfMonth) {
            y++; m = 0; d = 0
        }
        return Triple(y, m, d)
    }

    /** 详情页大数字点击循环的所有候选（第一项固定为天数） */
    fun detailNumbers(target: LocalDate, today: LocalDate, includeStartDay: Boolean): List<DetailNumber> {
        val dd = dayDiff(target, today)
        val abs = if (includeStartDay && dd < 0) -dd + 1 else kotlin.math.abs(dd)
        val list = mutableListOf(DetailNumber(abs.toString(), "day"))
        val (y, m, d) = ymd(target, today)
        if (y > 0) list.add(DetailNumber(y.toString(), if (m > 0) "year+$m" else "year"))
        val totalMonths = y * 12 + m
        if (totalMonths > 0) list.add(DetailNumber(totalMonths.toString(), if (d > 0) "month+$d" else "month"))
        if (abs >= 7) list.add(
            DetailNumber((abs / 7).toString(), if (abs % 7 > 0) "week+${abs % 7}" else "week")
        )
        return list
    }
}
