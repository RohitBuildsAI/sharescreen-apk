package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentNotifications(limit: Int): Flow<List<NotificationEntity>>

    @Query("""
        SELECT * FROM notifications 
        WHERE appName LIKE '%' || :query || '%' 
           OR title LIKE '%' || :query || '%' 
           OR text LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC
    """)
    fun searchNotifications(query: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getNotificationsByPackage(packageName: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getNotificationsBetween(startTime: Long, endTime: Long): Flow<List<NotificationEntity>>

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    @Query("SELECT COUNT(*) FROM notifications")
    fun getCount(): Flow<Int>

    @Query("SELECT DISTINCT appName FROM notifications ORDER BY appName ASC")
    fun getDistinctApps(): Flow<List<String>>
}
