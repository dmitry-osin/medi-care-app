package pro.osin.tools.medicare.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pro.osin.tools.medicare.data.database.entities.Medicine
import pro.osin.tools.medicare.data.database.entities.Reminder

@Database(
    entities = [Medicine::class, Reminder::class],
    version = 1,
    exportSchema = false
)
abstract class MedicineDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: MedicineDatabase? = null

        fun getDatabase(context: Context): MedicineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "medicare_database"
                )
                    .fallbackToDestructiveMigration() // For development - recreates DB on schema change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

