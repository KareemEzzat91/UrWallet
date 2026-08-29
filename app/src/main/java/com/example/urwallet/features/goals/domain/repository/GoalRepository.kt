package com.example.urwallet.features.goals.domain.repository

import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.model.GoalContribution
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(): Flow<List<Goal>>
    fun getActiveGoals(): Flow<List<Goal>>
    fun getNearestActiveGoal(): Flow<Goal?>
    fun getGoalById(id: Long): Flow<Goal?>
    fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>>
    fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double>
    suspend fun insertGoal(goal: Goal): Long
    suspend fun updateGoal(goal: Goal)
    suspend fun addContribution(goalId: Long, amount: Double, note: String? = null): Long
    suspend fun deleteGoal(id: Long)
}
