package com.example.urwallet.features.budgets.domain.repository

import com.example.urwallet.features.budgets.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getGlobalBudget(month: Int, year: Int): Flow<Budget?>
    fun getCategoryBudgets(month: Int, year: Int): Flow<List<Budget>>
    fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<Budget?>
    suspend fun insertOrUpdateBudget(budget: Budget): Long
    suspend fun deleteBudget(id: Long)
}
