package com.example.urwallet.features.challenges.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urwallet.features.challenges.data.entity.ChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: ChallengeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<ChallengeEntity>): List<Long>

    @Update
    suspend fun updateChallenge(challenge: ChallengeEntity): Int

    @Query("SELECT * FROM challenges ORDER BY isActive DESC, startDate DESC")
    fun getAllChallenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE isActive = 1 AND isCompleted = 0 ORDER BY startDate ASC")
    fun getActiveChallenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE isActive = 1 AND isCompleted = 0 ORDER BY startDate ASC LIMIT 1")
    fun getPrimaryActiveChallenge(): Flow<ChallengeEntity?>

    @Query("SELECT * FROM challenges WHERE id = :id LIMIT 1")
    fun getChallengeById(id: Long): Flow<ChallengeEntity?>

    @Query("DELETE FROM challenges WHERE id = :id")
    suspend fun deleteChallenge(id: Long): Int
}
