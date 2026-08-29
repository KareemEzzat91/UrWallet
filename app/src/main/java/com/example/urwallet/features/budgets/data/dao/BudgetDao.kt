package com.example.urwallet.features.budgets.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urwallet.features.budgets.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity): Int

    @Query("SELECT * FROM budgets WHERE categoryId IS NULL AND month = :month AND year = :year LIMIT 1")
    fun getGlobalBudget(month: Int, year: Int): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE categoryId IS NOT NULL AND month = :month AND year = :year")
    fun getCategoryBudgets(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year LIMIT 1")
    fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<BudgetEntity?>

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long): Int
}
