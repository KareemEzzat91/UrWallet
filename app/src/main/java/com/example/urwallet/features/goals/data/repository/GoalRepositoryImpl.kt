package com.example.urwallet.features.goals.data.repository

import com.example.urwallet.features.goals.data.dao.GoalContributionDao
import com.example.urwallet.features.goals.data.dao.GoalDao
import com.example.urwallet.features.goals.data.entity.GoalContributionEntity
import com.example.urwallet.features.goals.data.entity.GoalEntity
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.model.GoalContribution
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class GoalRepositoryImpl(
    private val goalDao: GoalDao,
    private val goalContributionDao: GoalContributionDao
) : GoalRepository {

    override fun getAllGoals(): Flow<List<Goal>> {
        return goalDao.getAllGoals().flatMapLatest { goals ->
            if (goals.isEmpty()) {
                flowOf(emptyList())
            } else {
                val goalFlows = goals.map { goalEntity ->
                    goalContributionDao.getTotalSavedForGoal(goalEntity.id).map { saved ->
                        goalEntity.toDomain(saved)
                    }
                }
                combine(goalFlows) { it.toList() }
            }
        }
    }

    override fun getActiveGoals(): Flow<List<Goal>> {
        return goalDao.getActiveGoals().flatMapLatest { goals ->
            if (goals.isEmpty()) {
                flowOf(emptyList())
            } else {
                val goalFlows = goals.map { goalEntity ->
                    goalContributionDao.getTotalSavedForGoal(goalEntity.id).map { saved ->
                        goalEntity.toDomain(saved)
                    }
                }
                combine(goalFlows) { it.toList() }
            }
        }
    }

    override fun getNearestActiveGoal(): Flow<Goal?> {
        return goalDao.getNearestActiveGoal().flatMapLatest { goalEntity ->
            if (goalEntity == null) {
                flowOf(null)
            } else {
                goalContributionDao.getTotalSavedForGoal(goalEntity.id).map { saved ->
                    goalEntity.toDomain(saved)
                }
            }
        }
    }

    override fun getGoalById(id: Long): Flow<Goal?> {
        return goalDao.getGoalById(id).flatMapLatest { goalEntity ->
            if (goalEntity == null) {
                flowOf(null)
            } else {
                goalContributionDao.getTotalSavedForGoal(goalEntity.id).map { saved ->
                    goalEntity.toDomain(saved)
                }
            }
        }
    }

    override fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>> {
        return goalContributionDao.getContributionsForGoal(goalId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double> {
        return goalContributionDao.getMonthlyContributionsSum(startDate, endDate)
    }

    override suspend fun insertGoal(goal: Goal): Long {
        return goalDao.insertGoal(goal.toEntity())
    }

    override suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal.toEntity())
    }

    override suspend fun addContribution(goalId: Long, amount: Double, note: String?): Long {
        val contributionId = goalContributionDao.insertContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = amount,
                note = note,
                date = System.currentTimeMillis()
            )
        )
        // Check goal completion
        val totalSaved = goalContributionDao.getTotalSavedForGoalSync(goalId)
        val goalEntity = goalDao.getGoalByIdSync(goalId)
        if (goalEntity != null && totalSaved >= goalEntity.targetAmount) {
            goalDao.updateGoalCompletion(goalId, true)
        }
        return contributionId
    }

    override suspend fun deleteGoal(id: Long) {
        goalDao.softDeleteGoal(id)
    }

    // --- Mappers ---
    private fun GoalEntity.toDomain(savedAmount: Double) = Goal(
        id = id,
        name = name,
        icon = icon,
        targetAmount = targetAmount,
        savedAmount = savedAmount,
        paceMode = paceMode,
        monthlyTarget = monthlyTarget,
        deadline = deadline,
        createdAt = createdAt,
        isCompleted = isCompleted || (savedAmount >= targetAmount),
        isDeleted = isDeleted
    )

    private fun Goal.toEntity() = GoalEntity(
        id = id,
        name = name,
        icon = icon,
        targetAmount = targetAmount,
        paceMode = paceMode,
        monthlyTarget = monthlyTarget,
        deadline = deadline,
        createdAt = createdAt,
        isCompleted = isCompleted,
        isDeleted = isDeleted
    )

    private fun GoalContributionEntity.toDomain() = GoalContribution(
        id = id,
        goalId = goalId,
        amount = amount,
        note = note,
        date = date
    )
}
