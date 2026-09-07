package com.example.urwallet.features.goals.domain.usecase

import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.model.GoalContribution
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContributeToGoalUseCaseTest {

    private lateinit var useCase: ContributeToGoalUseCase
    private var lastContributedAmount: Double = 0.0
    private var lastContributedNote: String? = null

    private val fakeGoalRepository = object : GoalRepository {
        override fun getAllGoals(): Flow<List<Goal>> = flowOf(emptyList())
        override fun getActiveGoals(): Flow<List<Goal>> = flowOf(emptyList())
        override fun getNearestActiveGoal(): Flow<Goal?> = flowOf(null)
        override fun getGoalById(id: Long): Flow<Goal?> = flowOf(null)
        override fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>> = flowOf(emptyList())
        override fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override suspend fun insertGoal(goal: Goal): Long = 1L
        override suspend fun updateGoal(goal: Goal) {}
        override suspend fun addContribution(goalId: Long, amount: Double, note: String?): Long {
            lastContributedAmount = amount
            lastContributedNote = note
            return 55L
        }
        override suspend fun deleteGoal(id: Long) {}
    }

    @Before
    fun setUp() {
        lastContributedAmount = 0.0
        lastContributedNote = null
        useCase = ContributeToGoalUseCase(fakeGoalRepository)
    }

    @Test
    fun `when amount is positive, contribution is saved and id returned`() = runBlocking {
        val result = useCase(goalId = 1L, amount = 1000.0, note = "مكافأة شهرية")
        assertTrue(result.isSuccess)
        assertEquals(55L, result.getOrNull())
        assertEquals(1000.0, lastContributedAmount, 0.001)
        assertEquals("مكافأة شهرية", lastContributedNote)
    }

    @Test
    fun `when amount is zero or negative, return failure`() = runBlocking {
        val zeroResult = useCase(goalId = 1L, amount = 0.0)
        assertTrue(zeroResult.isFailure)

        val negResult = useCase(goalId = 1L, amount = -250.0)
        assertTrue(negResult.isFailure)
    }
}
