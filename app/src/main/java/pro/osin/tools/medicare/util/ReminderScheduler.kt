package pro.osin.tools.medicare.util

import android.content.Context
import androidx.work.*
import androidx.work.workDataOf
import pro.osin.tools.medicare.data.database.entities.Reminder
import pro.osin.tools.medicare.domain.model.DayOfWeek
import pro.osin.tools.medicare.worker.ReminderWorker
import java.util.*
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME_PREFIX = "reminder_"

    fun scheduleReminder(context: Context, reminder: Reminder) {
        val workManager = WorkManager.getInstance(context)
        
        // Cancel old tasks for this reminder
        cancelReminder(context, reminder.id)

        if (!reminder.isActive) return

        // Schedule tasks for each day of week
        DayOfWeek.values().forEach { day ->
            val isDaySelected = when (day) {
                DayOfWeek.MONDAY -> reminder.monday
                DayOfWeek.TUESDAY -> reminder.tuesday
                DayOfWeek.WEDNESDAY -> reminder.wednesday
                DayOfWeek.THURSDAY -> reminder.thursday
                DayOfWeek.FRIDAY -> reminder.friday
                DayOfWeek.SATURDAY -> reminder.saturday
                DayOfWeek.SUNDAY -> reminder.sunday
            }

            if (isDaySelected) {
                scheduleForDay(context, reminder, day)
            }
        }
    }

    private fun scheduleForDay(context: Context, reminder: Reminder, day: DayOfWeek) {
        val workManager = WorkManager.getInstance(context)
        val timeParts = reminder.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        // Schedule tasks for several weeks ahead for reliability
        repeat(4) { weekOffset ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Set day of week
            val dayOfWeekCalendar = when (day) {
                DayOfWeek.MONDAY -> Calendar.MONDAY
                DayOfWeek.TUESDAY -> Calendar.TUESDAY
                DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
                DayOfWeek.THURSDAY -> Calendar.THURSDAY
                DayOfWeek.FRIDAY -> Calendar.FRIDAY
                DayOfWeek.SATURDAY -> Calendar.SATURDAY
                DayOfWeek.SUNDAY -> Calendar.SUNDAY
            }

            // Find next occurrence of this day of week
            val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            var daysUntilTarget = dayOfWeekCalendar - currentDayOfWeek
            if (daysUntilTarget < 0 || (daysUntilTarget == 0 && calendar.timeInMillis <= System.currentTimeMillis())) {
                daysUntilTarget += 7
            }
            daysUntilTarget += weekOffset * 7

            calendar.add(Calendar.DAY_OF_MONTH, daysUntilTarget)

            val delay = calendar.timeInMillis - System.currentTimeMillis()
            if (delay > 0) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresCharging(false)
                    .build()

                val inputData = workDataOf(
                    "reminderId" to reminder.id.toString()
                )

                val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .addTag("${WORK_NAME_PREFIX}${reminder.id}_${day.index}_${weekOffset}")
                    .build()

                workManager.enqueue(workRequest)
            }
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        val workManager = WorkManager.getInstance(context)
        // Cancel all tasks for all days of this reminder
        DayOfWeek.values().forEach { day ->
            repeat(4) { weekOffset ->
                workManager.cancelAllWorkByTag("${WORK_NAME_PREFIX}${reminderId}_${day.index}_${weekOffset}")
            }
        }
    }

    fun schedulePeriodicCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag("periodic_reminder_check")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_reminder_check",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}

