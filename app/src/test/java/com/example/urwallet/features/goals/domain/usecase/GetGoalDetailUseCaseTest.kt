package com.example.urwallet.features.goals.domain.usecase

import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.model.GoalContribution
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetGoalDetailUseCaseTest {

    private val now = System.currentTimeMillis()
    private val futureDeadline = now + (90L * 24 * 60 * 60 * 1000L) // 90 days

    private val testGoal = Goal(
        id = 10L,
        name = "صندوق الاستثمار",
        icon = "ic_goal_emergency",
        targetAmount = 10000.0,
        savedAmount = 4000.0,
        paceMode = GoalPaceMode.BALANCED,
        monthlyTarget = 1000.0,
        deadline = futureDeadline,
        createdAt = now - 10_000L
    )

    private val testContributions = listOf(
        GoalContribution(1L, 10L, 1000.0, "دفعة أولى", now - 5000L),
        GoalContribution(2L, 10L, 3000.0, "دفعة ثانية", now - 1000L)
    )

    private val fakeRepo = object : GoalRepository {
        override fun getAllGoals(): Flow<List<Goal>> = flowOf(emptyList())
        override fun getActiveGoals(): Flow<List<Goal>> = flowOf(emptyList())
        override fun getNearestActiveGoal(): Flow<Goal?> = flowOf(null)
        override fun getGoalById(id: Long): Flow<Goal?> = flowOf(if (id == 10L) testGoal else null)
        override fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>> =
            flowOf(if (goalId == 10L) testContributions else emptyList())
        override fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override suspend fun insertGoal(goal: Goal): Long = 1L
        override suspend fun updateGoal(goal: Goal) {}
        override suspend fun addContribution(goalId: Long, amount: Double, note: String?): Long = 1L
        override suspend fun deleteGoal(id: Long) {}
    }

    @Test
    fun `when goal exists, detail combines calculations and sorted contributions`() = runBlocking {
        val useCase = GetGoalDetailUseCase(fakeRepo)
        val detail = useCase(10L).first()

        assertNotNull(detail)
        assertEquals(10L, detail!!.goal.id)
        assertEquals(4000.0, detail.savedAmount, 0.001)
        assertEquals(6000.0, detail.remainingAmount, 0.001)
        assertEquals(40.0, detail.progressPercentage, 0.001)
        assertEquals(2, detail.contributions.size)

        // Sorted descending by date
        assertEquals(2L, detail.contributions[0].id)
        assertEquals(1L, detail.contributions[1].id)

        assertTrue(detail.daysRemaining > 0)
        assertTrue(detail.requiredMonthlySavings > 0.0)
    }

    @Test
    fun `when goal does not exist, detail returns null`() = runBlocking {
        val useCase = GetGoalDetailUseCase(fakeRepo)
        val detail = useCase(999L).first()
        assertNull(detail)
    }
}
