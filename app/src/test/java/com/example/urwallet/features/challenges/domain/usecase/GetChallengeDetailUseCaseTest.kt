package com.example.urwallet.features.challenges.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.challenges.domain.model.Challenge
import com.example.urwallet.features.challenges.domain.model.DayStatus
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GetChallengeDetailUseCaseTest {

    private val fakeChallengeRepository = FakeChallengeRepository()
    private val fakeTransactionRepository = FakeTransactionRepository()
    private val fakeGoalRepository = FakeGoalRepository()

    private val useCase = GetChallengeDetailUseCase(
        fakeChallengeRepository,
        fakeTransactionRepository,
        fakeGoalRepository
    )

    @Test
    fun `returns null when challengeId does not exist`() = runTest {
        val result = useCase(999L).first()
        assertNull(result)
    }

    @Test
    fun `returns evaluated challenge progress with daily timeline`() = runTest {
        val now = DateUtils.getStartOfDay()
        val oneDay = 24 * 60 * 60 * 1000L

        val challenge = Challenge(
            id = 5L,
            title = "Weekend Calm",
            description = "Test description",
            type = ChallengeType.NO_SPENDING,
            targetDays = 2,
            startDate = now,
            endDate = now + (2 * oneDay) - 1,
            isActive = true
        )

        fakeChallengeRepository.challengesFlow.value = listOf(challenge)

        val result = useCase(5L).first()
        assertNotNull(result)
        assertEquals(5L, result?.challenge?.id)
        assertEquals(2, result?.targetDays)
        assertEquals(2, result?.dailyStatuses?.size)
        assertEquals(DayStatus.TODAY, result?.dailyStatuses?.first()?.status)
    }

    private class FakeChallengeRepository : ChallengeRepository {
        val challengesFlow = MutableStateFlow<List<Challenge>>(emptyList())

        override fun getAllChallenges(): Flow<List<Challenge>> = challengesFlow
        override fun getActiveChallenges(): Flow<List<Challenge>> = challengesFlow
        override fun getPrimaryActiveChallenge(): Flow<Challenge?> = challengesFlow.map { it.firstOrNull() }
        override fun getChallengeById(id: Long): Flow<Challenge?> = challengesFlow.map { list -> list.find { it.id == id } }
        override suspend fun insertChallenge(challenge: Challenge): Long = 1L
        override suspend fun updateChallenge(challenge: Challenge) {}
        override suspend fun deleteChallenge(id: Long) {}
    }

    private class FakeTransactionRepository : TransactionRepository {
        override fun getAllTransactions(): Flow<List<Transaction>> = MutableStateFlow(emptyList())
        override fun getTransactionById(id: Long): Flow<Transaction?> = MutableStateFlow(null)
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = MutableStateFlow(emptyList())
        override fun getTransactionsBetween(startDate: Long, endDate: Long): Flow<List<Transaction>> = MutableStateFlow(emptyList())
        override fun getTransactionsByCategoryAndPeriod(categoryId: Long, startDate: Long, endDate: Long): Flow<List<Transaction>> = MutableStateFlow(emptyList())
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
