package com.example.urwallet.features.backup.domain.usecase

import com.example.urwallet.features.backup.domain.repository.BackupRepository
import javax.inject.Inject

class ClearAllDataUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.clearAllData()
}
