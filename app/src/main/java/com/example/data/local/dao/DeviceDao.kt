package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: DeviceEntity)

    @Query("SELECT * FROM devices ORDER BY lastConnected DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    fun getDeviceById(id: String): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices WHERE pairingStatus = 'PAIRED' ORDER BY lastConnected DESC")
    fun getPairedDevices(): Flow<List<DeviceEntity>>

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDevice(id: String)

    @Query("DELETE FROM devices")
    suspend fun clearAllDevices()
}
