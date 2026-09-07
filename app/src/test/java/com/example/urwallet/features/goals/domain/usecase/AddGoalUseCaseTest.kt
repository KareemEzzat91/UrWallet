package com.example.urwallet.features.goals.domain.usecase

import com.example.urwallet.core.common.GoalPaceMode
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

class AddGoalUseCaseTest {

    private lateinit var useCase: AddGoalUseCase
    private var insertedGoal: Goal? = null

    private val fakeGoalRepository = object : GoalRepository {
        override fun getAllGoals(): Flow<List<Goal>> = flowOf(emptyList())
        override fun getActiveGoals(): Flow<List<Goal>> = flowOf(emptyList())
        override fun getNearestActiveGoal(): Flow<Goal?> = flowOf(null)
        override fun getGoalById(id: Long): Flow<Goal?> = flowOf(null)
        override fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>> = flowOf(emptyList())
        override fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override suspend fun insertGoal(goal: Goal): Long {
            insertedGoal = goal
            return 101L
        }
        override suspend fun updateGoal(goal: Goal) {}
        override suspend fun addContribution(goalId: Long, amount: Double, note: String?): Long = 1L
        override suspend fun deleteGoal(id: Long) {}
    }

    @Before
    fun setUp() {
        insertedGoal = null
        useCase = AddGoalUseCase(fakeGoalRepository)
    }

    @Test
    fun `when name is blank, return failure`() = runBlocking {
        val future = System.currentTimeMillis() + 100_000_000L
        val result = useCase(
            name = "   ",
            targetAmount = 5000.0,
            deadline = future,
            paceMode = GoalPaceMode.BALANCED,
            icon = "ic_goal_custom"
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `when target amount is zero or negative, return failure`() = runBlocking {
        val future = System.currentTimeMillis() + 100_000_000L

        val zeroResult = useCase("سيارة", 0.0, future)
        assertTrue(zeroResult.isFailure)

        val negResult = useCase("سيارة", -500.0, future)
        assertTrue(negResult.isFailure)
    }

    @Test
    fun `when deadline is in the past, return failure`() = runBlocking {
        val past = System.currentTimeMillis() - 1000L
        val result = useCase("سفر", 10000.0, past)
        assertTrue(result.isFailure)
    }

    @Test
    fun `when inputs are valid, goal is inserted with calculated monthly target`() = runBlocking {
        val now = System.currentTimeMillis()
        val sixMonthsLater = now + (6L * 30 * 24 * 60 * 60 * 1000L)

        val result = useCase(
            name = "لابتوب جديد",
            targetAmount = 30000.0,
            deadline = sixMonthsLater,
            paceMode = GoalPaceMode.AGGRESSIVE,
            icon = "ic_goal_custom"
        )

        assertTrue(result.isSuccess)
        assertEquals(101L, result.getOrNull())
        assertEquals("لابتوب جديد", insertedGoal?.name)
        assertEquals(30000.0, insertedGoal?.targetAmount ?: 0.0, 0.001)
        assertTrue((insertedGoal?.monthlyTarget ?: 0.0) > 0.0)
    }
}
