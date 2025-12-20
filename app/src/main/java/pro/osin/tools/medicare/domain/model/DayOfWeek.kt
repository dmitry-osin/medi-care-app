package pro.osin.tools.medicare.domain.model

import java.util.Calendar

enum class DayOfWeek(val index: Int, val calendarDay: Int, val isWeekend: Boolean = false) {
    MONDAY(1, Calendar.MONDAY),
    TUESDAY(2, Calendar.TUESDAY),
    WEDNESDAY(3, Calendar.WEDNESDAY),
    THURSDAY(4, Calendar.THURSDAY),
    FRIDAY(5, Calendar.FRIDAY),
    SATURDAY(6, Calendar.SATURDAY, isWeekend = true),
    SUNDAY(7, Calendar.SUNDAY, isWeekend = true);

    fun getShortName(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendarDay)
        val dayName = calendar.getDisplayName(
            Calendar.DAY_OF_WEEK,
            Calendar.SHORT,
            java.util.Locale.getDefault()
        ) ?: ""
        return dayName
    }

    fun getFullName(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendarDay)
        val dayName = calendar.getDisplayName(
            Calendar.DAY_OF_WEEK,
            Calendar.LONG,
            java.util.Locale.getDefault()
        ) ?: ""
        return dayName
    }
}
