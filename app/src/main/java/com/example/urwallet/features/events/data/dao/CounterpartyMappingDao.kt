package com.example.urwallet.features.events.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.urwallet.features.events.data.entity.CounterpartyMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterpartyMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mapping: CounterpartyMappingEntity): Long

    @Query("SELECT * FROM counterparty_mappings WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getMappingByPhone(phoneNumber: String): CounterpartyMappingEntity?

    @Query("SELECT * FROM counterparty_mappings ORDER BY updatedAt DESC")
    fun getAllMappings(): Flow<List<CounterpartyMappingEntity>>

    @Query("DELETE FROM counterparty_mappings WHERE phoneNumber = :phoneNumber")
    suspend fun deleteMapping(phoneNumber: String): Int
}
