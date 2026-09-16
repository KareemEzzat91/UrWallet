package com.example.urwallet.features.backup.domain.usecase

import com.example.urwallet.features.backup.data.dto.BackupPayloadDto
import com.example.urwallet.features.backup.domain.model.BackupSummary
import com.example.urwallet.features.backup.domain.model.ImportStrategy
import com.example.urwallet.features.backup.domain.repository.BackupRepository
import javax.inject.Inject

class RestoreBackupUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(
        payload: BackupPayloadDto,
        strategy: ImportStrategy
    ): Result<BackupSummary> = repository.restoreBackup(payload, strategy)
}
