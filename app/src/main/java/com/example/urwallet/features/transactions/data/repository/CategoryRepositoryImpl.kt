package com.example.urwallet.features.transactions.data.repository

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.transactions.data.dao.CategoryDao
import com.example.urwallet.features.transactions.data.entity.CategoryEntity
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

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
