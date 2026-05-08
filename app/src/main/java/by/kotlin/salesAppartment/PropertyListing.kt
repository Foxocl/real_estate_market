package by.kotlin.salesAppartment

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "property_listings")
data class PropertyListing(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionType: String = "",
    val propertyType: String = "",
    val rooms: Int? = null,
    val floor: Int? = null,
    val country: String = "",
    val locality: String = "",
    val street: String = "",
    val houseNumber: String = "",
    val negotiable: Boolean = false,
    val price: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var latitude: Double? = null,
    var longitude: Double? = null,
    val imageUrls: List<String> = emptyList(),
    val userId: String? = null,
    val firestoreId: String? = null
)