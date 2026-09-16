package com.example.urwallet.features.backup.domain.usecase

import com.example.urwallet.features.backup.domain.repository.BackupRepository
import java.io.File
import javax.inject.Inject

class ExportTransactionsCsvUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    suspend operator fun invoke(): Result<File> = repository.exportTransactionsCsv()
}
