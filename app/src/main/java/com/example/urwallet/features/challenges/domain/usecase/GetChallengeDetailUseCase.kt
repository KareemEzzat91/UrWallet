package com.example.urwallet.features.challenges.domain.usecase

import com.example.urwallet.features.challenges.domain.calculator.ChallengeEvaluator
import com.example.urwallet.features.challenges.domain.model.ChallengeProgress
import com.example.urwallet.features.challenges.domain.repository.ChallengeRepository
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetChallengeDetailUseCase @Inject constructor(
    private val challengeRepository: ChallengeRepository,
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository
) {

    operator fun invoke(challengeId: Long): Flow<ChallengeProgress?> {
        return combine(
            challengeRepository.getChallengeById(challengeId),
            transactionRepository.getAllTransactions(),
            goalRepository.getAllGoals()
        ) { challenge, transactions, goals ->
            if (challenge == null) return@combine null

            val totalSavedAmount = goals.sumOf { it.savedAmount }
            ChallengeEvaluator.evaluate(
                challenge = challenge,
                transactions = transactions,
                savedAmount = totalSavedAmount
            )
        }
    }
}
