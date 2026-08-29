package com.example.urwallet.features.goals.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urwallet.features.goals.data.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity): Int

    @Query("SELECT * FROM goals WHERE isDeleted = 0 ORDER BY deadline ASC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT g.* FROM goals g WHERE g.isDeleted = 0 AND (SELECT COALESCE(SUM(c.amount), 0.0) FROM goal_contributions c WHERE c.goalId = g.id) < g.targetAmount ORDER BY g.deadline ASC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT g.* FROM goals g WHERE g.isDeleted = 0 AND (SELECT COALESCE(SUM(c.amount), 0.0) FROM goal_contributions c WHERE c.goalId = g.id) < g.targetAmount ORDER BY g.deadline ASC LIMIT 1")
    fun getNearestActiveGoal(): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id AND isDeleted = 0 LIMIT 1")
    fun getGoalById(id: Long): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getGoalByIdSync(id: Long): GoalEntity?

    @Query("UPDATE goals SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteGoal(id: Long): Int
}
