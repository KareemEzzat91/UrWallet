package com.example.urwallet.features.events.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.urwallet.features.events.data.entity.CategoryMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mapping: CategoryMappingEntity): Long

    @Query("SELECT * FROM category_mappings WHERE pattern = :pattern LIMIT 1")
    suspend fun getMapping(pattern: String): CategoryMappingEntity?

    @Query("SELECT * FROM category_mappings ORDER BY usageCount DESC, updatedAt DESC")
    fun getAllMappings(): Flow<List<CategoryMappingEntity>>

    @Query("SELECT * FROM category_mappings ORDER BY usageCount DESC, updatedAt DESC")
    suspend fun getAllMappingsSync(): List<CategoryMappingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<CategoryMappingEntity>): List<Long>

    @Query("DELETE FROM category_mappings WHERE pattern = :pattern")
    suspend fun deleteMapping(pattern: String): Int

    @Query("DELETE FROM category_mappings")
    suspend fun deleteAllMappings(): Int
}
