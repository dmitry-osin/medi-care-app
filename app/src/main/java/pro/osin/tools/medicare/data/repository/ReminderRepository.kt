package pro.osin.tools.medicare.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import pro.osin.tools.medicare.data.database.ReminderDao
import pro.osin.tools.medicare.data.database.entities.Reminder
import pro.osin.tools.medicare.util.ReminderScheduler

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val context: Context? = null
) {
    // Use applicationContext to avoid memory leaks
    private val applicationContext: Context?
        get() = context?.applicationContext
    fun getAllActiveReminders(): Flow<List<Reminder>> = reminderDao.getAllActiveReminders()

    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()

    fun getRemindersForMedicine(medicineId: Long): Flow<List<Reminder>> =
        reminderDao.getRemindersForMedicine(medicineId)

    suspend fun getReminderById(id: Long): Reminder? = reminderDao.getReminderById(id)

    suspend fun getActiveReminderCountForMedicine(medicineId: Long): Int =
        reminderDao.getActiveReminderCountForMedicine(medicineId)

    suspend fun insertReminder(reminder: Reminder): Long {
        val id = reminderDao.insertReminder(reminder)
        applicationContext?.let {
            if (reminder.isActive) {
                ReminderScheduler.scheduleReminder(it, reminder.copy(id = id))
            }
        }
        return id
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
        applicationContext?.let {
            if (reminder.isActive) {
                ReminderScheduler.scheduleReminder(it, reminder)
            } else {
                // Cancel notifications if reminder is deactivated
                ReminderScheduler.cancelReminder(it, reminder.id)
            }
        }
    }

    suspend fun deleteReminder(reminder: Reminder) {
        applicationContext?.let {
            ReminderScheduler.cancelReminder(it, reminder.id)
        }
        reminderDao.deleteReminder(reminder)
    }

    suspend fun getRemindersForDayAndTime(dayOfWeek: Int, time: String): List<Reminder> =
        reminderDao.getRemindersForDayAndTime(dayOfWeek, time)

    fun getRemindersForDay(dayOfWeek: Int): Flow<List<Reminder>> =
        reminderDao.getRemindersForDay(dayOfWeek)

    suspend fun deactivateRemindersForMedicine(medicineId: Long) {
        reminderDao.deactivateRemindersForMedicine(medicineId)
        applicationContext?.let {
            // Cancel notifications for all deactivated reminders
            val reminders = getRemindersForMedicine(medicineId).first()
            reminders.forEach { reminder ->
                if (!reminder.isActive) {
                    ReminderScheduler.cancelReminder(it, reminder.id)
                }
            }
        }
    }
}

