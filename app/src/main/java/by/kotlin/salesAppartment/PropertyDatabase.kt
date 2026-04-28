package by.kotlin.salesAppartment

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [PropertyListing::class, CachedGeocode::class],
    version = 3,
    exportSchema = false
)
abstract class PropertyDatabase : RoomDatabase() {
    abstract fun propertyListingDao(): PropertyListingDao
    abstract fun geocodeDao(): GeocodeDao
    companion object {
        @Volatile
        private var INSTANCE: PropertyDatabase? = null

        fun getInstance(context: Context): PropertyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PropertyDatabase::class.java,
                    "property_database"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}