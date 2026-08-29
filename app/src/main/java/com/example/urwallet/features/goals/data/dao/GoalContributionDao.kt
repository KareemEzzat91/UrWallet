package com.example.urwallet.features.goals.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.urwallet.features.goals.data.entity.GoalContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalContributionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: GoalContributionEntity): Long

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC")
    fun getContributionsForGoal(goalId: Long): Flow<List<GoalContributionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM goal_contributions WHERE goalId = :goalId")
    fun getTotalSavedForGoal(goalId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM goal_contributions WHERE goalId = :goalId")
    suspend fun getTotalSavedForGoalSync(goalId: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM goal_contributions WHERE date >= :startDate AND date <= :endDate")
    fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double>
}
