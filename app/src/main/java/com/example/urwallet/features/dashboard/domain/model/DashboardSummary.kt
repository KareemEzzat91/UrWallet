package com.example.urwallet.features.dashboard.domain.model

import com.example.urwallet.features.challenges.domain.model.ChallengeProgress
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction

data class DashboardTransactionItem(
    val transaction: Transaction,
    val category: Category? = null
)

data class DashboardSummary(
    val netBalance: Double,
    val monthlyIncome: Double,
    val monthlyExpense: Double,
    val recentTransactions: List<DashboardTransactionItem>,
    val nearestGoal: Goal?,
    val activeChallenge: ChallengeProgress? = null
)
