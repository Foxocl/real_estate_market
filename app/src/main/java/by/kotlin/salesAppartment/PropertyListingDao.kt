package by.kotlin.salesAppartment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyListingDao {
    @Insert
    suspend fun insert(listing: PropertyListing): Long

    @Update
    suspend fun update(listing: PropertyListing): Int

    @Delete
    suspend fun delete(listing: PropertyListing): Int

    @Query("SELECT * FROM property_listings ORDER BY createdAt DESC")
    fun getAllListings(): Flow<List<PropertyListing>>

    @Query("SELECT * FROM property_listings WHERE id = :id")
    suspend fun getListingById(id: Long): PropertyListing?

    @Query("SELECT COUNT(*) FROM property_listings")
    suspend fun getListingCount(): Int
}