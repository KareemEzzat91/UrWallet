package com.example.urwallet.features.transactions.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.FilterPeriod
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.model.TransactionFilterCriteria
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetFilteredTransactionsUseCaseTest {

    private lateinit var useCase: GetFilteredTransactionsUseCase

    private val foodCategory = Category(
        id = 1L,
        name = "أكل",
        type = CategoryType.EXPENSE,
        icon = "ic_food",
        color = "#FF5722",
        isDefault = true
    )

    private val transportCategory = Category(
        id = 2L,
        name = "مواصلات",
        type = CategoryType.EXPENSE,
        icon = "ic_transport",
        color = "#2196F3",
        isDefault = true
    )

    private val salaryCategory = Category(
        id = 3L,
        name = "راتب",
        type = CategoryType.INCOME,
        icon = "ic_salary",
        color = "#4CAF50",
        isDefault = true
    )

    private val categoryMap = listOf(foodCategory, transportCategory, salaryCategory).associateBy { it.id }

    private val sampleTransactions = listOf(
        Transaction(
            id = 101L,
            amount = 150.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "ستاربكس",
            note = "قهوة صباحية",
            date = System.currentTimeMillis()
        ),
        Transaction(
            id = 102L,
            amount = 50.0,
            type = TransactionType.EXPENSE,
            categoryId = 2L,
            title = "أوبر",
            note = "مشوار للعمل",
            date = System.currentTimeMillis() - 24 * 60 * 60 * 1000L // Yesterday
        ),
        Transaction(
            id = 103L,
            amount = 5000.0,
            type = TransactionType.INCOME,
            categoryId = 3L,
            title = "راتب شهر مايو",
            note = "تحويل بنكي",
            date = System.currentTimeMillis() - 48 * 60 * 60 * 1000L
        )
    )

    @Before
    fun setUp() {
        useCase = GetFilteredTransactionsUseCase()
    }

    @Test
    fun `default criteria returns all transactions`() {
        val results = useCase(sampleTransactions, categoryMap, TransactionFilterCriteria())
        assertEquals(3, results.size)
    }

    @Test
    fun `search by title finds matching transaction`() {
        val criteria = TransactionFilterCriteria(query = "ستاربكس")
        val results = useCase(sampleTransactions, categoryMap, criteria)

        assertEquals(1, results.size)
        assertEquals(101L, results[0].id)
    }

    @Test
    fun `search by note finds matching transaction`() {
        val criteria = TransactionFilterCriteria(query = "مشوار")
        val results = useCase(sampleTransactions, categoryMap, criteria)

        assertEquals(1, results.size)
        assertEquals(102L, results[0].id)
    }

    @Test
    fun `search by category name finds matching transactions`() {
        val criteria = TransactionFilterCriteria(query = "أكل")
        val results = useCase(sampleTransactions, categoryMap, criteria)

        assertEquals(1, results.size)
        assertEquals(101L, results[0].id)
    }

    @Test
    fun `filtering by expense type returns only expenses`() {
        val criteria = TransactionFilterCriteria(type = TransactionType.EXPENSE)
        val results = useCase(sampleTransactions, categoryMap, criteria)

        assertEquals(2, results.size)
        assertTrue(results.all { it.type == TransactionType.EXPENSE })
    }

    @Test
    fun `filtering by income type returns only income`() {
        val criteria = TransactionFilterCriteria(type = TransactionType.INCOME)
        val results = useCase(sampleTransactions, categoryMap, criteria)

        assertEquals(1, results.size)
        assertEquals(TransactionType.INCOME, results[0].type)
    }

    @Test
    fun `filtering by category ids returns matching transactions`() {
        val criteria = TransactionFilterCriteria(categoryIds = setOf(1L, 2L))
        val results = useCase(sampleTransactions, categoryMap, criteria)

        assertEquals(2, results.size)
        assertTrue(results.any { it.categoryId == 1L })
        assertTrue(results.any { it.categoryId == 2L })
    }

    @Test
    fun `filtering by amount bounds returns transactions within range`() {
        val criteria = TransactionFilterCriteria(minAmount = 100.0, maxAmount = 200.0)
        val results = useCase(sampleTransactions, categoryMap, criteria)

        assertEquals(1, results.size)
        assertEquals(150.0, results[0].amount, 0.001)
    }

    @Test
    fun `combined criteria applies all filters together`() {
        val criteria = TransactionFilterCriteria(
            query = "ستاربكس",
            type = TransactionType.EXPENSE,
            categoryIds = setOf(1L),
            minAmount = 100.0
        )
        val results = useCase(sampleTransactions, categoryMap, criteria)

        assertEquals(1, results.size)
        assertEquals(101L, results[0].id)
    }

    @Test
    fun `search with no matches returns empty list`() {
        val criteria = TransactionFilterCriteria(query = "غير موجود نهائيا")
        val results = useCase(sampleTransactions, categoryMap, criteria)

        assertTrue(results.isEmpty())
    }
}
