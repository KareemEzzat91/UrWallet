package com.example.urwallet.features.challenges.data.repository

import com.example.urwallet.features.challenges.data.dao.ChallengeDao
import com.example.urwallet.features.challenges.data.entity.ChallengeEntity
import com.example.urwallet.features.challenges.domain.model.Challenge
import com.example.urwallet.features.challenges.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChallengeRepositoryImpl @Inject constructor(
    private val challengeDao: ChallengeDao
) : ChallengeRepository {

    override fun getAllChallenges(): Flow<List<Challenge>> {
        return challengeDao.getAllChallenges().map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveChallenges(): Flow<List<Challenge>> {
        return challengeDao.getActiveChallenges().map { list -> list.map { it.toDomain() } }
    }

    override fun getPrimaryActiveChallenge(): Flow<Challenge?> {
        return challengeDao.getPrimaryActiveChallenge().map { it?.toDomain() }
    }

    override fun getChallengeById(id: Long): Flow<Challenge?> {
        return challengeDao.getChallengeById(id).map { it?.toDomain() }
    }

    override suspend fun insertChallenge(challenge: Challenge): Long {
        return challengeDao.insertChallenge(challenge.toEntity())
    }

    override suspend fun updateChallenge(challenge: Challenge) {
        challengeDao.updateChallenge(challenge.toEntity())
    }

    override suspend fun deleteChallenge(id: Long) {
        challengeDao.deleteChallenge(id)
    }

    // --- Mappers ---
    private fun ChallengeEntity.toDomain() = Challenge(
        id = id,
        title = title,
        description = description,
        type = type,
        targetAmount = targetAmount,
        targetDays = targetDays,
        categoryId = categoryId,
        startDate = startDate,
        endDate = endDate,
        currentProgress = currentProgress,
        streakDays = streakDays,
        isCompleted = isCompleted,
        isActive = isActive
    )

    private fun Challenge.toEntity() = ChallengeEntity(
        id = id,
        title = title,
        description = description,
        type = type,
        targetAmount = targetAmount,
        targetDays = targetDays,
        categoryId = categoryId,
        startDate = startDate,
        endDate = endDate,
        currentProgress = currentProgress,
        streakDays = streakDays,
        isCompleted = isCompleted,
        isActive = isActive
    )
}
