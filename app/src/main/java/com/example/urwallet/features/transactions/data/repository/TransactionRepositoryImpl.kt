package com.example.urwallet.features.transactions.data.repository

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.data.dao.CategoryDao
import com.example.urwallet.features.transactions.data.dao.TransactionDao
import com.example.urwallet.features.transactions.data.entity.CategoryEntity
import com.example.urwallet.features.transactions.data.entity.TransactionEntity
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionById(id: Long): Flow<Transaction?> {
        return transactionDao.getTransactionById(id).map { it?.toDomain() }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBetween(startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByCategoryAndPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategoryAndPeriod(categoryId, startDate, endDate)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun getSumByTypeAndPeriod(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<Double> {
        return transactionDao.getSumByTypeAndPeriod(type, startDate, endDate)
    }

    override fun getTotalSumByType(type: TransactionType): Flow<Double> {
        return transactionDao.getTotalSumByType(type)
    }

    override fun getSumByCategoryAndPeriod(
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<Double> {
        return transactionDao.getSumByCategoryAndPeriod(categoryId, startDate, endDate)
    }

    override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> {
        return transactionDao.getTodayTransactionCount(startOfDay, endOfDay)
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(id: Long) {
        categoryDao.softDeleteCategory(id)
    }

    // --- Mappers ---
    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        amount = amount,
        type = type,
        categoryId = categoryId,
        title = title,
        note = note,
        date = date,
        receiptPath = receiptPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        amount = amount,
        type = type,
        categoryId = categoryId,
        title = title,
        note = note,
        date = date,
        receiptPath = receiptPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        type = type,
        icon = icon,
        color = color,
        isDefault = isDefault,
        isDeleted = isDeleted
    )

    private fun Category.toEntity() = CategoryEntity(
        id = id,
        name = name,
        type = type,
        icon = icon,
        color = color,
        isDefault = isDefault,
        isDeleted = isDeleted
    )
}
