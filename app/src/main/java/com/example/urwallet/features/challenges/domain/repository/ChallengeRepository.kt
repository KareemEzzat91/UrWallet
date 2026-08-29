package com.example.urwallet.features.challenges.domain.repository

import com.example.urwallet.features.challenges.domain.model.Challenge
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {
    fun getAllChallenges(): Flow<List<Challenge>>
    fun getActiveChallenges(): Flow<List<Challenge>>
    fun getPrimaryActiveChallenge(): Flow<Challenge?>
    fun getChallengeById(id: Long): Flow<Challenge?>
    suspend fun insertChallenge(challenge: Challenge): Long
    suspend fun updateChallenge(challenge: Challenge)
    suspend fun deleteChallenge(id: Long)
}
