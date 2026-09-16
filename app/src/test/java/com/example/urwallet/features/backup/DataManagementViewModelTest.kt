package com.example.urwallet.features.backup

import com.example.urwallet.features.backup.data.dto.BackupDataDto
import com.example.urwallet.features.backup.data.dto.BackupPayloadDto
import com.example.urwallet.features.backup.domain.model.BackupSummary
import com.example.urwallet.features.backup.domain.model.BackupValidationResult
import com.example.urwallet.features.backup.domain.model.ImportStrategy
import com.example.urwallet.features.backup.domain.repository.BackupRepository
import com.example.urwallet.features.backup.domain.usecase.ClearAllDataUseCase
import com.example.urwallet.features.backup.domain.usecase.ExportJsonBackupUseCase
import com.example.urwallet.features.backup.domain.usecase.ExportTransactionsCsvUseCase
import com.example.urwallet.features.backup.domain.usecase.RestoreBackupUseCase
import com.example.urwallet.features.backup.domain.usecase.ValidateBackupFileUseCase
import com.example.urwallet.features.backup.presentation.DataManagementViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBackupRepository
    private lateinit var viewModel: DataManagementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBackupRepository()
        viewModel = DataManagementViewModel(
            exportJsonBackupUseCase = ExportJsonBackupUseCase(fakeRepository),
            exportTransactionsCsvUseCase = ExportTransactionsCsvUseCase(fakeRepository),
            validateBackupFileUseCase = ValidateBackupFileUseCase(fakeRepository),
            restoreBackupUseCase = RestoreBackupUseCase(fakeRepository),
            clearAllDataUseCase = ClearAllDataUseCase(fakeRepository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exportJsonBackup_success_updatesUiStateWithShareFile() = runTest(testDispatcher) {
        val testFile = File("fake_backup.json")
        fakeRepository.exportJsonResult = Result.success(testFile)

        viewModel.exportJsonBackup()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testFile, state.shareFile)
        assertEquals("application/json", state.shareMimeType)
        assertFalse(state.isErrorMessage)
        assertNotNull(state.userMessage)
    }

    @Test
    fun exportTransactionsCsv_success_updatesUiStateWithCsvFile() = runTest(testDispatcher) {
        val testFile = File("fake_tx.csv")
        fakeRepository.exportCsvResult = Result.success(testFile)

        viewModel.exportTransactionsCsv()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testFile, state.shareFile)
        assertEquals("text/csv", state.shareMimeType)
        assertFalse(state.isErrorMessage)
    }

    @Test
    fun onFileContentRead_validJson_setsPendingRestorePayload() = runTest(testDispatcher) {
        val summary = BackupSummary(1, 1, 1, 1, 1, 5, 1)
        val payload = BackupPayloadDto(version = 1, data = BackupDataDto())
        fakeRepository.validationResult = BackupValidationResult.Valid(summary, payload)

        viewModel.onFileContentRead("{\"valid\": true}")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(payload, state.pendingRestorePayload)
        assertEquals(summary, state.pendingValidationSummary)
    }

    @Test
    fun onFileContentRead_invalidJson_setsErrorMessage() = runTest(testDispatcher) {
        fakeRepository.validationResult = BackupValidationResult.Invalid("الملف غير متوافق")

        viewModel.onFileContentRead("bad json")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.pendingRestorePayload)
        assertTrue(state.isErrorMessage)
        assertEquals("الملف غير متوافق", state.userMessage)
    }

    @Test
    fun restoreBackup_success_clearsPendingAndShowsMessage() = runTest(testDispatcher) {
        val summary = BackupSummary(1, 1, 1, 1, 1, 10, 1)
        val payload = BackupPayloadDto(version = 1, data = BackupDataDto())
        fakeRepository.validationResult = BackupValidationResult.Valid(summary, payload)
        fakeRepository.restoreResult = Result.success(summary)

        viewModel.onFileContentRead("valid json")
        advanceUntilIdle()

        viewModel.restoreBackup(ImportStrategy.REPLACE_ALL)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.pendingRestorePayload)
        assertFalse(state.isErrorMessage)
        assertTrue(state.userMessage?.contains("تم استرجاع البيانات بنجاح") == true)
    }

    @Test
    fun clearAllData_success_showsSuccessMessage() = runTest(testDispatcher) {
        fakeRepository.clearResult = Result.success(Unit)

        viewModel.clearAllData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isErrorMessage)
        assertTrue(state.userMessage?.contains("تم حذف جميع البيانات") == true)
    }

    @Test
    fun dismissStrategyDialog_clearsPendingPayload() = runTest(testDispatcher) {
        val summary = BackupSummary(1, 1, 1, 1, 1, 5, 1)
        val payload = BackupPayloadDto(version = 1, data = BackupDataDto())
        fakeRepository.validationResult = BackupValidationResult.Valid(summary, payload)

        viewModel.onFileContentRead("valid json")
        advanceUntilIdle()

        viewModel.dismissStrategyDialog()

        val state = viewModel.uiState.value
        assertNull(state.pendingRestorePayload)
        assertNull(state.pendingValidationSummary)
    }
}

private class FakeBackupRepository : BackupRepository {
    var exportJsonResult: Result<File> = Result.success(File("backup.json"))
    var exportCsvResult: Result<File> = Result.success(File("tx.csv"))
    var validationResult: BackupValidationResult = BackupValidationResult.Invalid("Not set")
    var restoreResult: Result<BackupSummary> = Result.success(BackupSummary(0, 0, 0, 0, 0, 0, 0))
    var clearResult: Result<Unit> = Result.success(Unit)

    override suspend fun exportJsonBackup(): Result<File> = exportJsonResult
    override suspend fun exportTransactionsCsv(): Result<File> = exportCsvResult
    override suspend fun validateBackupJson(jsonContent: String): BackupValidationResult = validationResult
    override suspend fun restoreBackup(payload: BackupPayloadDto, strategy: ImportStrategy): Result<BackupSummary> = restoreResult
    override suspend fun clearAllData(): Result<Unit> = clearResult
}
