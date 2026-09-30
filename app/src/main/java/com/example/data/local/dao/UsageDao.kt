package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: UsageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUsage(usages: List<UsageEntity>)

    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY usageDuration DESC")
    fun getUsageForDate(date: String): Flow<List<UsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY usageDuration DESC")
    fun getUsageBetweenDates(startDate: String, endDate: String): Flow<List<UsageEntity>>

    @Query("SELECT * FROM app_usage ORDER BY lastUsed DESC")
    fun getAllUsage(): Flow<List<UsageEntity>>

    @Query("SELECT * FROM app_usage WHERE appName LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%' ORDER BY lastUsed DESC")
    fun searchUsage(query: String): Flow<List<UsageEntity>>

    @Query("SELECT SUM(usageDuration) FROM app_usage WHERE date = :date")
    fun getTotalUsageDurationForDate(date: String): Flow<Long?>

    @Query("DELETE FROM app_usage WHERE date = :date")
    suspend fun clearUsageForDate(date: String)

    @Query("DELETE FROM app_usage")
    suspend fun clearAllUsage()
}
