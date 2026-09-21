package com.subzero.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MandateEntity::class, BlockedTrapEntity::class],
    version = 3,
    exportSchema = false
)
abstract class SubZeroDatabase : RoomDatabase() {
    abstract fun mandateDao(): MandateDao

    companion object {
        @Volatile
        private var INSTANCE: SubZeroDatabase? = null

        fun getInstance(context: Context): SubZeroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SubZeroDatabase::class.java,
                    "subzero_guardian.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
