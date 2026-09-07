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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetGoalsUseCaseTest {

    private val now = System.currentTimeMillis()

    private val goal1Active = Goal(
        id = 1L,
        name = "صندوق طوارئ",
        icon = "ic_goal_emergency",
        targetAmount = 10000.0,
        savedAmount = 2000.0, // 20% -> active
        paceMode = GoalPaceMode.BALANCED,
        monthlyTarget = 833.0,
        deadline = now + 100_000L,
        createdAt = now - 50_000L
    )

    private val goal2ActiveNearer = Goal(
        id = 2L,
        name = "شراء هاتف",
        icon = "ic_goal_custom",
        targetAmount = 5000.0,
        savedAmount = 1000.0, // 20% -> active, deadline sooner
        paceMode = GoalPaceMode.AGGRESSIVE,
        monthlyTarget = 1000.0,
        deadline = now + 50_000L,
        createdAt = now - 40_000L
    )

    private val goal3Completed = Goal(
        id = 3L,
        name = "سفر",
        icon = "ic_goal_travel",
        targetAmount = 8000.0,
        savedAmount = 8000.0, // 100% -> completed
        paceMode = GoalPaceMode.BALANCED,
        monthlyTarget = 1000.0,
        deadline = now + 200_000L,
        createdAt = now - 30_000L
    )

    private val fakeRepo = object : GoalRepository {
        override fun getAllGoals(): Flow<List<Goal>> = flowOf(listOf(goal1Active, goal2ActiveNearer, goal3Completed))
        override fun getActiveGoals(): Flow<List<Goal>> = flowOf(emptyList())
        override fun getNearestActiveGoal(): Flow<Goal?> = flowOf(null)
        override fun getGoalById(id: Long): Flow<Goal?> = flowOf(null)
        override fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>> = flowOf(emptyList())
        override fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override suspend fun insertGoal(goal: Goal): Long = 1L
        override suspend fun updateGoal(goal: Goal) {}
        override suspend fun addContribution(goalId: Long, amount: Double, note: String?): Long = 1L
        override suspend fun deleteGoal(id: Long) {}
    }

    @Test
    fun `goals are separated into active and completed correctly`() = runBlocking {
        val useCase = GetGoalsUseCase(fakeRepo)
        val result = useCase().first()

        assertEquals(2, result.activeGoals.size)
        assertEquals(1, result.completedGoals.size)

        // Goal 2 has earlier deadline (now + 50_000) than Goal 1 (now + 100_000)
        assertEquals(2L, result.activeGoals[0].id)
        assertEquals(1L, result.activeGoals[1].id)

        // Completed goal
        assertEquals(3L, result.completedGoals[0].id)
        assertTrue(result.completedGoals[0].isCompleted)
        assertEquals(100.0, result.completedGoals[0].progressPercentage, 0.001)
    }
}
