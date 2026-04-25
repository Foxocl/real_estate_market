package by.kotlin.salesAppartment

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "property_listings")
data class PropertyListing(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Transaction type
    val transactionType: String,

    // Property type
    val propertyType: String,

    // Rooms count
    val rooms: Int? = null,

    // Floor number
    val floor: Int? = null,

    // Address components
    val locality: String,
    val street: String,
    val houseNumber: String,

    val negotiable: Boolean,

    val price: Double? = null,

    val createdAt: Long = System.currentTimeMillis()
)