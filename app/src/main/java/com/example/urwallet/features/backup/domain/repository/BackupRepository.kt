package com.example.urwallet.features.backup.domain.repository

import com.example.urwallet.features.backup.data.dto.BackupPayloadDto
import com.example.urwallet.features.backup.domain.model.BackupSummary
import com.example.urwallet.features.backup.domain.model.BackupValidationResult
import com.example.urwallet.features.backup.domain.model.ImportStrategy
import java.io.File

interface BackupRepository {
    suspend fun exportJsonBackup(): Result<File>
    suspend fun exportTransactionsCsv(): Result<File>
    suspend fun validateBackupJson(jsonContent: String): BackupValidationResult
    suspend fun restoreBackup(payload: BackupPayloadDto, strategy: ImportStrategy): Result<BackupSummary>
    suspend fun clearAllData(): Result<Unit>
}
