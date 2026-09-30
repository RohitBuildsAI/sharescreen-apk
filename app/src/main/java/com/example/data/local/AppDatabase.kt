package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.DeviceDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.dao.UsageDao
import com.example.data.local.entity.DeviceEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.UsageEntity

@Database(
    entities = [
        NotificationEntity::class,
        UsageEntity::class,
        DeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao
    abstract fun usageDao(): UsageDao
    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phone_companion.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
