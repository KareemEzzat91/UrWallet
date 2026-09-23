package com.example.urwallet.features.events

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.CategoryMapping
import com.example.urwallet.features.events.domain.model.CategorySuggestionSource
import com.example.urwallet.features.events.domain.usecase.SuggestCategoryUseCase
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SuggestCategoryUseCaseTest {

    private lateinit var fakeEventRepo: FakeFinancialEventRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var suggestCategoryUseCase: SuggestCategoryUseCase

    @Before
    fun setUp() {
        fakeEventRepo = FakeFinancialEventRepository()
        fakeCategoryRepo = FakeCategoryRepository(
            listOf(
                Category(id = 1L, name = "طعام ومشروبات", type = CategoryType.EXPENSE, icon = "ic_food", color = "#FF5722"),
                Category(id = 2L, name = "بقالة وسوبرماركت", type = CategoryType.EXPENSE, icon = "ic_cart", color = "#4CAF50"),
                Category(id = 3L, name = "مواصلات وتنقل", type = CategoryType.EXPENSE, icon = "ic_car", color = "#2196F3"),
                Category(id = 4L, name = "صحة وأدوية", type = CategoryType.EXPENSE, icon = "ic_health", color = "#E91E63"),
                Category(id = 5L, name = "فواتير وخدمات", type = CategoryType.EXPENSE, icon = "ic_bill", color = "#9C27B0"),
                Category(id = 6L, name = "راتب ودخل", type = CategoryType.INCOME, icon = "ic_salary", color = "#009688")
            )
        )
        suggestCategoryUseCase = SuggestCategoryUseCase(fakeEventRepo, fakeCategoryRepo)
    }

    @Test
    fun `Tier 1 - learned local mapping takes highest priority`() = runTest {
        // Teach local mapping for "Starbucks" to Category 2 (Grocery) instead of food
        fakeEventRepo.saveCategoryMapping(
            CategoryMapping(pattern = "starbucks", categoryId = 2L, usageCount = 5)
        )

        val suggestion = suggestCategoryUseCase(
            merchantOrCounterparty = "Starbucks",
            rawMessage = "Purchase at Starbucks with card *1234",
            type = TransactionType.EXPENSE
        )

        assertNotNull(suggestion)
        assertEquals(2L, suggestion!!.categoryId)
        assertEquals(CategorySuggestionSource.LEARNED_MAPPING, suggestion.source)
        assertEquals(0.95f, suggestion.confidence, 0.01f)
    }

    @Test
    fun `Tier 2 - keyword rules match Egyptian supermarkets to groceries`() = runTest {
        val suggestion = suggestCategoryUseCase(
            merchantOrCounterparty = "Carrefour City Center",
            rawMessage = "شراء بمبلغ 350 ج.م لدى كارفور",
            type = TransactionType.EXPENSE
        )

        assertNotNull(suggestion)
        assertEquals(2L, suggestion!!.categoryId)
        assertEquals("بقالة وسوبرماركت", suggestion.categoryName)
        assertEquals(CategorySuggestionSource.KEYWORD_RULES, suggestion.source)
    }

    @Test
    fun `Tier 2 - keyword rules match ride hailing to transport`() = runTest {
        val suggestion = suggestCategoryUseCase(
            merchantOrCounterparty = "Uber Trip",
            rawMessage = "تم خصم 75 ج.م رحلة أوبر",
            type = TransactionType.EXPENSE
        )

        assertNotNull(suggestion)
        assertEquals(3L, suggestion!!.categoryId)
        assertEquals("مواصلات وتنقل", suggestion.categoryName)
        assertEquals(CategorySuggestionSource.KEYWORD_RULES, suggestion.source)
    }

    @Test
    fun `Tier 2 - keyword rules match pharmacy to health`() = runTest {
        val suggestion = suggestCategoryUseCase(
            merchantOrCounterparty = "صيدلية العزبي",
            rawMessage = "شراء من صيدلية العزبي بقيمة 220 ج.م",
            type = TransactionType.EXPENSE
        )

        assertNotNull(suggestion)
        assertEquals(4L, suggestion!!.categoryId)
        assertEquals("صحة وأدوية", suggestion.categoryName)
        assertEquals(CategorySuggestionSource.KEYWORD_RULES, suggestion.source)
    }

    @Test
    fun `returns null when no keyword or learned pattern matches`() = runTest {
        val suggestion = suggestCategoryUseCase(
            merchantOrCounterparty = "Unknown Vendor XYZ",
            rawMessage = "Payment to XYZ 100 EGP",
            type = TransactionType.EXPENSE
        )

        assertNull(suggestion)
    }

    private class FakeCategoryRepository(private val categories: List<Category>) : CategoryRepository {
        override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> =
            flowOf(categories.filter { it.type == type })
        override suspend fun getCategoryById(id: Long): Category? = categories.find { it.id == id }
        override suspend fun insertCategory(category: Category): Long = category.id
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }
}
