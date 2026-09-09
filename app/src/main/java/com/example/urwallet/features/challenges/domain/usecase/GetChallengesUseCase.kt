package com.example.urwallet.features.challenges.domain.usecase

import com.example.urwallet.features.challenges.domain.calculator.ChallengeEvaluator
import com.example.urwallet.features.challenges.domain.model.ChallengeProgress
import com.example.urwallet.features.challenges.domain.repository.ChallengeRepository
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class ChallengesResult(
    val activeChallenges: List<ChallengeProgress>,
    val completedChallenges: List<ChallengeProgress>
)

class GetChallengesUseCase @Inject constructor(
    private val challengeRepository: ChallengeRepository,
    private val transactionRepository: TransactionRepository,
    private val goalRepository: GoalRepository
) {

    operator fun invoke(): Flow<ChallengesResult> {
        return combine(
            challengeRepository.getAllChallenges(),
            transactionRepository.getAllTransactions(),
            goalRepository.getAllGoals()
        ) { challenges, transactions, goals ->
            val totalSavedAmount = goals.sumOf { it.savedAmount }

            val evaluatedChallenges = challenges.map { challenge ->
                ChallengeEvaluator.evaluate(
                    challenge = challenge,
                    transactions = transactions,
                    savedAmount = totalSavedAmount
                )
            }

            val active = evaluatedChallenges.filter { it.challenge.isActive && !it.isCompleted }
            val completed = evaluatedChallenges.filter { it.isCompleted || it.challenge.isCompleted }

            ChallengesResult(
                activeChallenges = active,
                completedChallenges = completed
            )
        }
    }
}
