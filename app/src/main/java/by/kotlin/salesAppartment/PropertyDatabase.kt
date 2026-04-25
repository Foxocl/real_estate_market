package by.kotlin.salesAppartment

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [PropertyListing::class],
    version = 1,
    exportSchema = false
)
abstract class PropertyDatabase : RoomDatabase() {
    abstract fun propertyListingDao(): PropertyListingDao

    companion object {
        @Volatile
        private var INSTANCE: PropertyDatabase? = null

        fun getInstance(context: Context): PropertyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PropertyDatabase::class.java,
                    "property_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}