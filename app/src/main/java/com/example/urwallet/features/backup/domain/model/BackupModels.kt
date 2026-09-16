package com.example.urwallet.features.backup.domain.model

import com.example.urwallet.features.backup.data.dto.BackupPayloadDto

enum class ImportStrategy {
    REPLACE_ALL,
    MERGE
}

data class BackupSummary(
    val categoriesCount: Int,
    val goalsCount: Int,
    val contributionsCount: Int,
    val budgetsCount: Int,
    val recurringCount: Int,
    val transactionsCount: Int,
    val challengesCount: Int,
    val exportedAt: Long = System.currentTimeMillis()
)

sealed class BackupValidationResult {
    data class Valid(val summary: BackupSummary, val payload: BackupPayloadDto) : BackupValidationResult()
    data class Invalid(val errorMessage: String) : BackupValidationResult()
}
