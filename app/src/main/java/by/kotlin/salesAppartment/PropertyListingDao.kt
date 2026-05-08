package by.kotlin.salesAppartment

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyListingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Query("SELECT * FROM property_listings WHERE userId = :userId ORDER BY createdAt DESC")
    fun getListingsByUser(userId: String): Flow<List<PropertyListing>>

    @Query("SELECT * FROM property_listings WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getByFirestoreId(firestoreId: String): PropertyListing?

    @Transaction
    suspend fun upsert(listing: PropertyListing) {
        val existing = listing.firestoreId?.let { getByFirestoreId(it) }
        if (existing != null) {
            update(listing.copy(id = existing.id))
        } else {
            insert(listing)
        }
    }
}