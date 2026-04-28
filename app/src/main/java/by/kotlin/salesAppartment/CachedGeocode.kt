package by.kotlin.salesAppartment

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_geocode")
data class CachedGeocode(
    @PrimaryKey
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)