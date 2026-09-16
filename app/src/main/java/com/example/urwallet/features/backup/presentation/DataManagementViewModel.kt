package com.example.urwallet.features.backup.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urwallet.features.backup.data.dto.BackupPayloadDto
import com.example.urwallet.features.backup.domain.model.BackupValidationResult
import com.example.urwallet.features.backup.domain.model.ImportStrategy
import com.example.urwallet.features.backup.domain.usecase.ClearAllDataUseCase
import com.example.urwallet.features.backup.domain.usecase.ExportJsonBackupUseCase
import com.example.urwallet.features.backup.domain.usecase.ExportTransactionsCsvUseCase
import com.example.urwallet.features.backup.domain.usecase.RestoreBackupUseCase
import com.example.urwallet.features.backup.domain.usecase.ValidateBackupFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val exportJsonBackupUseCase: ExportJsonBackupUseCase,
    private val exportTransactionsCsvUseCase: ExportTransactionsCsvUseCase,
    private val validateBackupFileUseCase: ValidateBackupFileUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val clearAllDataUseCase: ClearAllDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()

    fun exportJsonBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = exportJsonBackupUseCase()
            result.onSuccess { file ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        shareFile = file,
                        shareMimeType = "application/json",
                        shareTitle = "مشاركة النسخة الاحتياطية",
                        userMessage = "تم إنشاء النسخة الاحتياطية بنجاح",
                        isErrorMessage = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = error.message ?: "فشل إنشاء النسخة الاحتياطية",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun exportTransactionsCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = exportTransactionsCsvUseCase()
            result.onSuccess { file ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        shareFile = file,
                        shareMimeType = "text/csv",
                        shareTitle = "مشاركة سجل المعاملات",
                        userMessage = "تم تصدير ملف CSV بنجاح",
                        isErrorMessage = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = error.message ?: "فشل تصدير ملف CSV",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun onFileContentRead(jsonString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val validation = validateBackupFileUseCase(jsonString)) {
                is BackupValidationResult.Valid -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingRestorePayload = validation.payload,
                            pendingValidationSummary = validation.summary
                        )
                    }
                }
                is BackupValidationResult.Invalid -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = validation.errorMessage,
                            isErrorMessage = true
                        )
                    }
                }
            }
        }
    }

    fun onFileReadError(message: String) {
        _uiState.update {
            it.copy(
                userMessage = message,
                isErrorMessage = true
            )
        }
    }

    fun restoreBackup(strategy: ImportStrategy) {
        val payload = _uiState.value.pendingRestorePayload ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, pendingRestorePayload = null, pendingValidationSummary = null) }
            val result = restoreBackupUseCase(payload, strategy)
            result.onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "تم استرجاع البيانات بنجاح (${summary.transactionsCount} معاملة، ${summary.goalsCount} هدف)",
                        isErrorMessage = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = error.message ?: "فشل استرجاع البيانات",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun dismissStrategyDialog() {
        _uiState.update {
            it.copy(
                pendingRestorePayload = null,
                pendingValidationSummary = null
            )
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = clearAllDataUseCase()
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "تم حذف جميع البيانات المالية وإعادة بذر التصنيفات الافتراضية",
                        isErrorMessage = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = error.message ?: "فشل حذف البيانات",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun onShareHandled() {
        _uiState.update {
            it.copy(
                shareFile = null,
                shareMimeType = null,
                shareTitle = null
            )
        }
    }

    fun onMessageShown() {
        _uiState.update {
            it.copy(
                userMessage = null,
                isErrorMessage = false
            )
        }
    }
}
