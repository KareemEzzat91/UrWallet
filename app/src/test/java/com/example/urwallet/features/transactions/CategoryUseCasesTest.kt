package com.example.urwallet.features.transactions

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import com.example.urwallet.features.transactions.domain.usecase.AddCategoryUseCase
import com.example.urwallet.features.transactions.domain.usecase.DeleteCategoryUseCase
import com.example.urwallet.features.transactions.domain.usecase.UpdateCategoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategoryUseCasesTest {

    private lateinit var fakeRepository: FakeCategoryRepository
    private lateinit var addUseCase: AddCategoryUseCase
    private lateinit var updateUseCase: UpdateCategoryUseCase
    private lateinit var deleteUseCase: DeleteCategoryUseCase

    private class FakeCategoryRepository : CategoryRepository {
        val categories = mutableMapOf<Long, Category>()
        private var idCounter = 1L

        override fun getAllCategories(): Flow<List<Category>> =
            flowOf(categories.values.filter { !it.isDeleted })

        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
            flowOf(categories.values.filter { !it.isDeleted && (it.type == type || it.type == CategoryType.BOTH) })

        override suspend fun getCategoryById(id: Long): Category? =
            categories[id]?.takeIf { !it.isDeleted }

        override suspend fun insertCategory(category: Category): Long {
            val assignedId = if (category.id > 0) category.id else idCounter++
            categories[assignedId] = category.copy(id = assignedId)
            return assignedId
        }

        override suspend fun updateCategory(category: Category) {
            categories[category.id] = category
        }

        override suspend fun deleteCategory(id: Long) {
            val existing = categories[id]
            if (existing != null) {
                categories[id] = existing.copy(isDeleted = true)
            }
        }
    }

    @Before
    fun setUp() {
        fakeRepository = FakeCategoryRepository()
        addUseCase = AddCategoryUseCase(fakeRepository)
        updateUseCase = UpdateCategoryUseCase(fakeRepository)
        deleteUseCase = DeleteCategoryUseCase(fakeRepository)

        // Seed a default system category
        fakeRepository.categories[1L] = Category(
            id = 1L,
            name = "طعام",
            type = CategoryType.EXPENSE,
            icon = "ic_food",
            color = "#FF7043",
            isDefault = true,
            isDeleted = false
        )
    }

    @Test
    fun `addCategory with valid name and type succeeds and creates custom category`() = runTest {
        val result = addUseCase("مشتريات منزلية", CategoryType.EXPENSE, "ic_shopping", "#AB47BC")
        assertTrue(result.isSuccess)
        val id = result.getOrNull()!!

        val stored = fakeRepository.categories[id]
        assertEquals("مشتريات منزلية", stored?.name)
        assertEquals(CategoryType.EXPENSE, stored?.type)
        assertFalse(stored!!.isDefault)
        assertFalse(stored.isDeleted)
    }

    @Test
    fun `addCategory with blank name returns failure`() = runTest {
        val result = addUseCase("   ", CategoryType.EXPENSE)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `updateCategory with valid parameters modifies existing custom category`() = runTest {
        val addResult = addUseCase("مشروع جانبي", CategoryType.INCOME, "ic_freelance", "#00897B")
        val id = addResult.getOrNull()!!

        val updateResult = updateUseCase(id, "عمل إضافي", CategoryType.INCOME, "ic_salary", "#00732C")
        assertTrue(updateResult.isSuccess)

        val updated = fakeRepository.categories[id]
        assertEquals("عمل إضافي", updated?.name)
        assertEquals("ic_salary", updated?.icon)
        assertEquals("#00732C", updated?.color)
    }

    @Test
    fun `updateCategory with non-existent ID returns failure`() = runTest {
        val updateResult = updateUseCase(999L, "تعديل", CategoryType.EXPENSE, "ic_other", "#78909C")
        assertTrue(updateResult.isFailure)
    }

    @Test
    fun `deleteCategory on custom category performs soft delete without destroying records`() = runTest {
        val addResult = addUseCase("تصنيف مؤقت", CategoryType.EXPENSE, "ic_other", "#78909C")
        val id = addResult.getOrNull()!!

        val deleteResult = deleteUseCase(id)
        assertTrue(deleteResult.isSuccess)

        val stored = fakeRepository.categories[id]
        // Record still exists in storage for foreign key safety, but isDeleted is true
        assertTrue(stored!!.isDeleted)
    }

    @Test
    fun `deleteCategory on default system category fails and protects default categories`() = runTest {
        val deleteResult = deleteUseCase(1L) // ID 1 is seeded default
        assertTrue(deleteResult.isFailure)
        assertTrue(deleteResult.exceptionOrNull() is IllegalStateException)

        val stored = fakeRepository.categories[1L]
        assertFalse(stored!!.isDeleted) // Protected!
    }
}
