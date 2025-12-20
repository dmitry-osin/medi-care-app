package pro.osin.tools.medicare.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val type: String, // MedicineType.name
    val dosage: String,
    val quantity: String,
    val startDate: Long, // timestamp
    val endDate: Long? = null, // timestamp, nullable
    val color: Int, // color value (ARGB)
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

