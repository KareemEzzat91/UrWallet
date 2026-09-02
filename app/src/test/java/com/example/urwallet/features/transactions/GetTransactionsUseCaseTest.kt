package com.example.urwallet.features.transactions

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.usecase.GetTransactionsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTransactionsUseCaseTest {

    private lateinit var fakeRepository: FakeTransactionRepository
    private lateinit var useCase: GetTransactionsUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeTransactionRepository()
        useCase = GetTransactionsUseCase(fakeRepository)
    }

    @Test
    fun `when repository has transactions, returns them in flow`() = runTest {
        fakeRepository.insertTransaction(
            Transaction(
                id = 1L,
                amount = 150.0,
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                title = "فطور",
                date = System.currentTimeMillis()
            )
        )
        fakeRepository.insertTransaction(
            Transaction(
                id = 2L,
                amount = 5000.0,
                type = TransactionType.INCOME,
                categoryId = 2L,
                title = "راتب",
                date = System.currentTimeMillis()
            )
        )

        val transactions = useCase().first()
        assertEquals(2, transactions.size)
        assertEquals("فطور", transactions[0].title)
        assertEquals("راتب", transactions[1].title)
    }

    @Test
    fun `when repository is empty, returns empty list`() = runTest {
        val transactions = useCase().first()
        assertEquals(0, transactions.size)
    }
}
