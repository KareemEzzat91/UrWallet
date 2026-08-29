package com.example.urwallet.features.transactions.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.transactions.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: CategoryEntity): Int

    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE (type = :type OR type = 'BOTH') AND isDeleted = 0 ORDER BY id ASC")
    fun getCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("UPDATE categories SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteCategory(id: Long): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}
