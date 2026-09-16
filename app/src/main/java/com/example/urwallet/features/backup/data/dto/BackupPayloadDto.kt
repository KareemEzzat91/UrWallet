package com.example.urwallet.features.backup.data.dto

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.core.common.TransactionType

data class BackupPayloadDto(
    val version: Int = CURRENT_BACKUP_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val appName: String = "UrWallet",
    val appVersion: String = "1.0",
    val data: BackupDataDto
) {
    companion object {
        const val CURRENT_BACKUP_VERSION = 1
    }
}

data class BackupDataDto(
    val categories: List<CategoryBackupDto> = emptyList(),
    val goals: List<GoalBackupDto> = emptyList(),
    val goalContributions: List<GoalContributionBackupDto> = emptyList(),
    val budgets: List<BudgetBackupDto> = emptyList(),
    val recurringTransactions: List<RecurringTransactionBackupDto> = emptyList(),
    val transactions: List<TransactionBackupDto> = emptyList(),
    val challenges: List<ChallengeBackupDto> = emptyList()
)

data class CategoryBackupDto(
    val id: Long,
    val name: String,
    val type: CategoryType,
    val icon: String,
    val color: String,
    val isDefault: Boolean = false,
    val isDeleted: Boolean = false
)

data class GoalBackupDto(
    val id: Long,
    val name: String,
    val icon: String,
    val targetAmount: Double,
    val paceMode: GoalPaceMode,
    val monthlyTarget: Double,
    val deadline: Long,
    val createdAt: Long,
    val isDeleted: Boolean = false
)

data class GoalContributionBackupDto(
    val id: Long,
    val goalId: Long,
    val amount: Double,
    val note: String? = null,
    val date: Long
)

data class BudgetBackupDto(
    val id: Long,
    val categoryId: Long? = null,
    val amount: Double,
    val month: Int,
    val year: Int,
    val alertThreshold: Double = 0.80,
    val createdAt: Long
)

data class RecurringTransactionBackupDto(
    val id: Long,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val frequency: Frequency,
    val startDate: Long,
    val endDate: Long? = null,
    val nextOccurrence: Long,
    val isActive: Boolean = true,
    val createdAt: Long
)

data class TransactionBackupDto(
    val id: Long,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val title: String,
    val note: String? = null,
    val date: Long,
    val receiptPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class ChallengeBackupDto(
    val id: Long,
    val title: String,
    val description: String,
    val type: ChallengeType,
    val targetAmount: Double? = null,
    val targetDays: Int? = null,
    val categoryId: Long? = null,
    val startDate: Long,
    val endDate: Long,
    val currentProgress: Double = 0.0,
    val streakDays: Int = 0,
    val isCompleted: Boolean = false,
    val isActive: Boolean = true
)
