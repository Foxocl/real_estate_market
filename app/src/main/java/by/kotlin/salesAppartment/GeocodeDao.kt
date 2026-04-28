package by.kotlin.salesAppartment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GeocodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: CachedGeocode)

    @Query("SELECT * FROM cached_geocode WHERE address = :address")
    suspend fun getByAddress(address: String): CachedGeocode?
}