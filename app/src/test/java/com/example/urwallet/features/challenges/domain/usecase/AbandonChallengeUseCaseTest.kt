package com.example.urwallet.features.challenges.domain.usecase

import com.example.urwallet.features.challenges.domain.model.Challenge
import com.example.urwallet.features.challenges.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AbandonChallengeUseCaseTest {

    private val fakeRepository = FakeChallengeRepository()
    private val useCase = AbandonChallengeUseCase(fakeRepository)

    @Test
    fun `valid challengeId calls repository deleteChallenge`() = runTest {
        useCase(10L)
        assertEquals(listOf(10L), fakeRepository.deletedIds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid challengeId throws exception`() = runTest {
        useCase(0L)
    }

    private class FakeChallengeRepository : ChallengeRepository {
        val deletedIds = mutableListOf<Long>()

        override fun getAllChallenges(): Flow<List<Challenge>> = MutableStateFlow(emptyList())
        override fun getActiveChallenges(): Flow<List<Challenge>> = MutableStateFlow(emptyList())
        override fun getPrimaryActiveChallenge(): Flow<Challenge?> = MutableStateFlow(null)
        override fun getChallengeById(id: Long): Flow<Challenge?> = MutableStateFlow(null)
        override suspend fun insertChallenge(challenge: Challenge): Long = 1L
        override suspend fun updateChallenge(challenge: Challenge) {}

        override suspend fun deleteChallenge(id: Long) {
            deletedIds.add(id)
        }
    }
}
