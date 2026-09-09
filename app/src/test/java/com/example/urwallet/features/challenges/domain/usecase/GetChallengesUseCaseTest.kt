package com.example.urwallet.features.challenges.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.challenges.domain.model.Challenge
import com.example.urwallet.features.challenges.domain.repository.ChallengeRepository
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.model.GoalContribution
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetChallengesUseCaseTest {

    private val fakeChallengeRepository = FakeChallengeRepository()
    private val fakeTransactionRepository = FakeTransactionRepository()
    private val fakeGoalRepository = FakeGoalRepository()

    private val useCase = GetChallengesUseCase(
        fakeChallengeRepository,
        fakeTransactionRepository,
        fakeGoalRepository
    )

    @Test
    fun `combines challenges and correctly separates active from completed`() = runTest {
        val now = DateUtils.getStartOfDay()
        val oneDay = 24 * 60 * 60 * 1000L

        // Active challenge
        val activeChallenge = Challenge(
            id = 1L,
            title = "Active No Spend",
            description = "Test",
            type = ChallengeType.NO_SPENDING,
            targetDays = 5,
            startDate = now,
            endDate = now + (5 * oneDay),
            isActive = true
        )

        // Completed challenge (ended yesterday with 0 expenses)
        val completedChallenge = Challenge(
            id = 2L,
            title = "Completed No Spend",
            description = "Test",
            type = ChallengeType.NO_SPENDING,
            targetDays = 2,
            startDate = now - (3 * oneDay),
            endDate = now - (1 * oneDay),
            isActive = true
        )

        fakeChallengeRepository.challengesFlow.value = listOf(activeChallenge, completedChallenge)

        val result = useCase().first()

        assertEquals(1, result.activeChallenges.size)
        assertEquals(1L, result.activeChallenges.first().challenge.id)

        assertEquals(1, result.completedChallenges.size)
        assertEquals(2L, result.completedChallenges.first().challenge.id)
        assertTrue(result.completedChallenges.first().isCompleted)
    }

    @Test
    fun `when transactions are added, challenge progress updates reactively`() = runTest {
        val now = DateUtils.getStartOfDay()
        val oneDay = 24 * 60 * 60 * 1000L

        val challenge = Challenge(
            id = 3L,
            title = "Reduce Shopping",
            description = "Ceiling 500",
            type = ChallengeType.REDUCE_CATEGORY,
            targetAmount = 500.0,
            categoryId = 10L,
            startDate = now,
            endDate = now + (7 * oneDay),
            isActive = true
        )

        fakeChallengeRepository.challengesFlow.value = listOf(challenge)
        fakeTransactionRepository.transactionsFlow.value = emptyList()

        val initial = useCase().first()
        assertEquals(0.0, initial.activeChallenges.first().currentProgress, 0.01)

        // Add a shopping transaction
        fakeTransactionRepository.transactionsFlow.value = listOf(
            Transaction(
                id = 100L,
                title = "Clothes",
                amount = 250.0,
                type = TransactionType.EXPENSE,
                categoryId = 10L,
                date = now + 1000L
            )
        )

        val updated = useCase().first()
        assertEquals(250.0, updated.activeChallenges.first().currentProgress, 0.01)
        assertEquals(50.0, updated.activeChallenges.first().progressPercentage, 0.01)
    }

    private class FakeChallengeRepository : ChallengeRepository {
        val challengesFlow = MutableStateFlow<List<Challenge>>(emptyList())

        override fun getAllChallenges(): Flow<List<Challenge>> = challengesFlow
        override fun getActiveChallenges(): Flow<List<Challenge>> = challengesFlow
        override fun getPrimaryActiveChallenge(): Flow<Challenge?> = challengesFlow.map { it.firstOrNull() }
        override fun getChallengeById(id: Long): Flow<Challenge?> = challengesFlow.map { list -> list.find { it.id == id } }

        override suspend fun insertChallenge(challenge: Challenge): Long {
            val list = challengesFlow.value.toMutableList()
            val id = if (challenge.id == 0L) (list.size + 1).toLong() else challenge.id
            list.add(challenge.copy(id = id))
            challengesFlow.value = list
            return id
        }

        override suspend fun updateChallenge(challenge: Challenge) {
            val list = challengesFlow.value.toMutableList()
            val index = list.indexOfFirst { it.id == challenge.id }
            if (index != -1) {
                list[index] = challenge
                challengesFlow.value = list
            }
        }

        override suspend fun deleteChallenge(id: Long) {
            val list = challengesFlow.value.toMutableList()
            list.removeAll { it.id == id }
            challengesFlow.value = list
        }
    }

    private class FakeTransactionRepository : TransactionRepository {
        val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

        override fun getAllTransactions(): Flow<List<Transaction>> = transactionsFlow
        override fun getTransactionById(id: Long): Flow<Transaction?> = MutableStateFlow(null)
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = transactionsFlow
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = transactionsFlow
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = transactionsFlow
        override fun getSumByTypeAndPeriod(type: TransactionType, startDate: Long, endDate: Long): Flow<Double> = MutableStateFlow(0.0)
        override fun getTotalSumByType(type: TransactionType): Flow<Double> = MutableStateFlow(0.0)
        override fun getSumByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<Double> = MutableStateFlow(0.0)
        override fun getTodayTransactionCount(startOfDay: Long, endOfDay: Long): Flow<Int> = MutableStateFlow(0)
        override suspend fun insertTransaction(transaction: Transaction): Long = 1L
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override fun getAllCategories(): Flow<List<Category>> = MutableStateFlow(emptyList())
        override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> = MutableStateFlow(emptyList())
        override suspend fun getCategoryById(id: Long): Category? = null
        override suspend fun insertCategory(category: Category): Long = 1L
        override suspend fun updateCategory(category: Category) {}
        override suspend fun deleteCategory(id: Long) {}
    }

    private class FakeGoalRepository : GoalRepository {
        override fun getAllGoals(): Flow<List<Goal>> = MutableStateFlow(emptyList())
        override fun getActiveGoals(): Flow<List<Goal>> = MutableStateFlow(emptyList())
        override fun getNearestActiveGoal(): Flow<Goal?> = MutableStateFlow(null)
        override fun getGoalById(id: Long): Flow<Goal?> = MutableStateFlow(null)
        override fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>> = MutableStateFlow(emptyList())
        override fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double> = MutableStateFlow(0.0)
        override suspend fun insertGoal(goal: Goal): Long = 1L
        override suspend fun updateGoal(goal: Goal) {}
        override suspend fun addContribution(goalId: Long, amount: Double, note: String?): Long = 1L
        override suspend fun deleteGoal(id: Long) {}
    }
}
