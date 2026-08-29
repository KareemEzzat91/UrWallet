package com.example.urwallet.features.budgets.data.repository

import com.example.urwallet.features.budgets.data.dao.BudgetDao
import com.example.urwallet.features.budgets.data.entity.BudgetEntity
import com.example.urwallet.features.budgets.domain.model.Budget
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getGlobalBudget(month: Int, year: Int): Flow<Budget?> {
        return budgetDao.getGlobalBudget(month, year).map { it?.toDomain() }
    }

    override fun getCategoryBudgets(month: Int, year: Int): Flow<List<Budget>> {
        return budgetDao.getCategoryBudgets(month, year).map { list -> list.map { it.toDomain() } }
    }

    override fun getBudgetForCategory(categoryId: Long, month: Int, year: Int): Flow<Budget?> {
        return budgetDao.getBudgetForCategory(categoryId, month, year).map { it?.toDomain() }
    }

    override suspend fun insertOrUpdateBudget(budget: Budget): Long {
        return budgetDao.insertBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(id: Long) {
        budgetDao.deleteBudget(id)
    }

    // --- Mappers ---
    private fun BudgetEntity.toDomain() = Budget(
        id = id,
        categoryId = categoryId,
        amount = amount,
        month = month,
        year = year,
        alertThreshold = alertThreshold,
        createdAt = createdAt
    )

    private fun Budget.toEntity() = BudgetEntity(
        id = id,
        categoryId = categoryId,
        amount = amount,
        month = month,
        year = year,
        alertThreshold = alertThreshold,
        createdAt = createdAt
    )
}
