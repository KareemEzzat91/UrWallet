package com.example.urwallet.features.backup.presentation

import com.example.urwallet.features.backup.data.dto.BackupPayloadDto
import com.example.urwallet.features.backup.domain.model.BackupSummary
import java.io.File

data class DataManagementUiState(
    val isLoading: Boolean = false,
    val pendingRestorePayload: BackupPayloadDto? = null,
    val pendingValidationSummary: BackupSummary? = null,
    val userMessage: String? = null,
    val isErrorMessage: Boolean = false,
    val shareFile: File? = null,
    val shareMimeType: String? = null,
    val shareTitle: String? = null
)
