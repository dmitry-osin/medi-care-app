package pro.osin.tools.medicare.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pro.osin.tools.medicare.data.database.entities.Reminder

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY time")
    fun getAllActiveReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders ORDER BY time")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE medicineId = :medicineId ORDER BY time")
    fun getRemindersForMedicine(medicineId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Query("SELECT COUNT(*) FROM reminders WHERE medicineId = :medicineId AND isActive = 1")
    suspend fun getActiveReminderCountForMedicine(medicineId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isActive = 0 WHERE medicineId = :medicineId AND isActive = 1")
    suspend fun deactivateRemindersForMedicine(medicineId: Long)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    // Get active reminders for a specific day of week and time
    @Query("SELECT * FROM reminders WHERE isActive = 1 AND time = :time AND (CASE :dayOfWeek WHEN 1 THEN monday WHEN 2 THEN tuesday WHEN 3 THEN wednesday WHEN 4 THEN thursday WHEN 5 THEN friday WHEN 6 THEN saturday WHEN 7 THEN sunday END) = 1")
    suspend fun getRemindersForDayAndTime(dayOfWeek: Int, time: String): List<Reminder>

    // Get all active reminders for a specific day of week
    @Query("SELECT * FROM reminders WHERE isActive = 1 AND (CASE :dayOfWeek WHEN 1 THEN monday WHEN 2 THEN tuesday WHEN 3 THEN wednesday WHEN 4 THEN thursday WHEN 5 THEN friday WHEN 6 THEN saturday WHEN 7 THEN sunday END) = 1 ORDER BY time")
    fun getRemindersForDay(dayOfWeek: Int): Flow<List<Reminder>>
}

