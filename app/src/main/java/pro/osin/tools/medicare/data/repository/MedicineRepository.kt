package pro.osin.tools.medicare.data.repository

import kotlinx.coroutines.flow.Flow
import pro.osin.tools.medicare.data.database.MedicineDao
import pro.osin.tools.medicare.data.database.entities.Medicine

class MedicineRepository(private val medicineDao: MedicineDao) {
    fun getAllActiveMedicines(): Flow<List<Medicine>> = medicineDao.getAllActiveMedicines()

    fun getAllMedicines(): Flow<List<Medicine>> = medicineDao.getAllMedicines()

    suspend fun getMedicineById(id: Long): Medicine? = medicineDao.getMedicineById(id)

    suspend fun insertMedicine(medicine: Medicine): Long = medicineDao.insertMedicine(medicine)

    suspend fun updateMedicine(medicine: Medicine) = medicineDao.updateMedicine(medicine)

    suspend fun deleteMedicine(medicine: Medicine) = medicineDao.deleteMedicine(medicine)

    fun getMedicinesForDate(currentDate: Long): Flow<List<Medicine>> =
        medicineDao.getMedicinesForDate(currentDate)
}

