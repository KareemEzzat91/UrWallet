package com.example.urwallet.features.challenges.domain.usecase

import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.features.challenges.domain.model.Challenge
import com.example.urwallet.features.challenges.domain.model.ChallengePreset
import com.example.urwallet.features.challenges.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JoinChallengeUseCaseTest {

    private val fakeRepository = FakeChallengeRepository()
    private val useCase = JoinChallengeUseCase(fakeRepository)

    @Test
    fun `valid preset creates challenge with correct dates and persists to repository`() = runTest {
        val preset = ChallengePreset(
            id = "test_preset",
            title = "Test Challenge",
            description = "Test Description",
            type = ChallengeType.NO_SPENDING,
            targetDays = 7,
            difficulty = "متوسط",
            durationText = "7 أيام"
        )

        val fixedStart = DateUtils.getStartOfDay()
        val id = useCase(preset = preset, customStartDate = fixedStart)

        assertEquals(1L, id)
        assertEquals(1, fakeRepository.insertedChallenges.size)

        val inserted = fakeRepository.insertedChallenges.first()
        assertEquals("Test Challenge", inserted.title)
        assertEquals(ChallengeType.NO_SPENDING, inserted.type)
        assertEquals(7, inserted.targetDays)
        assertEquals(fixedStart, inserted.startDate)
        val expectedEnd = fixedStart + (7L * 24L * 60L * 60L * 1000L) - 1L
        assertEquals(expectedEnd, inserted.endDate)
        assertTrue(inserted.isActive)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `preset with zero or negative targetDays throws exception`() = runTest {
        val invalidPreset = ChallengePreset(
            id = "invalid",
            title = "Invalid",
            description = "Invalid",
            type = ChallengeType.NO_SPENDING,
            targetDays = 0,
            difficulty = "سهل",
            durationText = "0"
        )

        useCase(preset = invalidPreset)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SAVE_AMOUNT preset without targetAmount throws exception`() = runTest {
        val invalidPreset = ChallengePreset(
            id = "invalid_save",
            title = "Save Challenge",
            description = "Test",
            type = ChallengeType.SAVE_AMOUNT,
            targetDays = 30,
            targetAmount = null,
            difficulty = "متوسط",
            durationText = "30"
        )

        useCase(preset = invalidPreset)
    }

    private class FakeChallengeRepository : ChallengeRepository {
        val insertedChallenges = mutableListOf<Challenge>()

        override fun getAllChallenges(): Flow<List<Challenge>> = MutableStateFlow(insertedChallenges)
        override fun getActiveChallenges(): Flow<List<Challenge>> = MutableStateFlow(insertedChallenges)
        override fun getPrimaryActiveChallenge(): Flow<Challenge?> = MutableStateFlow(insertedChallenges.firstOrNull())
        override fun getChallengeById(id: Long): Flow<Challenge?> = MutableStateFlow(insertedChallenges.find { it.id == id })

        override suspend fun insertChallenge(challenge: Challenge): Long {
            val id = (insertedChallenges.size + 1).toLong()
            insertedChallenges.add(challenge.copy(id = id))
            return id
        }

        override suspend fun updateChallenge(challenge: Challenge) {}
        override suspend fun deleteChallenge(id: Long) {}
    }
}
