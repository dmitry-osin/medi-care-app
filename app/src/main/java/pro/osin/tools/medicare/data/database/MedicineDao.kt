package pro.osin.tools.medicare.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pro.osin.tools.medicare.data.database.entities.Medicine

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY name")
    fun getAllActiveMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines ORDER BY name")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Long): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    // Get medicines for current day (considering start/end dates)
    @Query("SELECT * FROM medicines WHERE isActive = 1 AND startDate <= :currentDate AND (endDate IS NULL OR endDate >= :currentDate) ORDER BY name")
    fun getMedicinesForDate(currentDate: Long): Flow<List<Medicine>>
}

