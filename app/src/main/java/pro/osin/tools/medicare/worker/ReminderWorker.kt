package pro.osin.tools.medicare.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import pro.osin.tools.medicare.data.database.MedicineDatabase
import pro.osin.tools.medicare.data.database.entities.Reminder
import pro.osin.tools.medicare.data.repository.MedicineRepository
import pro.osin.tools.medicare.data.repository.ReminderRepository
import pro.osin.tools.medicare.util.NotificationHelper
import java.util.*

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = MedicineDatabase.getDatabase(applicationContext)
        val reminderRepository = ReminderRepository(database.reminderDao(), applicationContext)
        val medicineRepository = MedicineRepository(database.medicineDao())

        try {
            // If there's a specific reminderId in data, send notification for it
            val reminderIdString = inputData.getString("reminderId")
            if (reminderIdString != null) {
                val reminderId = reminderIdString.toLong()
                val reminder = reminderRepository.getReminderById(reminderId)
                if (reminder != null && reminder.isActive) {
                    val medicine = medicineRepository.getMedicineById(reminder.medicineId)
                    if (medicine != null && medicine.isActive) {
                        NotificationHelper.showNotification(
                            context = applicationContext,
                            reminderId = reminder.id,
                            medicineName = medicine.name,
                            dosage = medicine.dosage,
                            quantity = medicine.quantity,
                            time = reminder.time
                        )
                    }
                }
            } else {
                // Periodic check - check all active reminders for current time
                val calendar = Calendar.getInstance()
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val dayOfWeekIndex = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
                val currentTime = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))

                val reminders = reminderRepository.getRemindersForDayAndTime(dayOfWeekIndex, currentTime)
                
                reminders.forEach { reminder ->
                    val medicine = medicineRepository.getMedicineById(reminder.medicineId)
                    if (medicine != null && medicine.isActive && reminder.isActive) {
                        NotificationHelper.showNotification(
                            context = applicationContext,
                            reminderId = reminder.id,
                            medicineName = medicine.name,
                            dosage = medicine.dosage,
                            quantity = medicine.quantity,
                            time = reminder.time
                        )
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            // WorkManager will automatically retry with exponential backoff
            // For non-critical errors, retry. For critical errors (like database issues),
            // we could return Result.failure() to stop retries, but retry is safer for notifications
            // WorkManager has built-in limits on retries (default is 10 attempts)
            return Result.retry()
        }
    }
}

