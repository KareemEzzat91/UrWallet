package com.example.urwallet.features.backup.domain.usecase

import com.example.urwallet.features.backup.domain.model.BackupValidationResult
import com.example.urwallet.features.backup.domain.repository.BackupRepository
import javax.inject.Inject

class ValidateBackupFileUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(jsonContent: String): BackupValidationResult =
        repository.validateBackupJson(jsonContent)
}
