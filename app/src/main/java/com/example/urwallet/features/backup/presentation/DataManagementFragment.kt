package com.example.urwallet.features.backup.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentDataManagementBinding
import com.example.urwallet.features.backup.domain.model.BackupSummary
import com.example.urwallet.features.backup.domain.model.ImportStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@AndroidEntryPoint
class DataManagementFragment : Fragment() {

    private var _binding: FragmentDataManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DataManagementViewModel by viewModels()

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            readFileFromUri(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeUiState()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnExportJsonBackup.setOnClickListener {
            viewModel.exportJsonBackup()
        }

        binding.btnImportJsonBackup.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
        }

        binding.btnExportCsv.setOnClickListener {
            viewModel.exportTransactionsCsv()
        }

        binding.btnClearAllData.setOnClickListener {
            showClearDataConfirmationDialog()
        }
    }

    private fun readFileFromUri(uri: Uri) {
        try {
            val content = requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            }
            if (!content.isNullOrBlank()) {
                viewModel.onFileContentRead(content)
            } else {
                viewModel.onFileReadError(getString(R.string.error_file_read))
            }
        } catch (e: Exception) {
            viewModel.onFileReadError(e.message ?: getString(R.string.error_file_read))
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.layoutLoading.isVisible = state.isLoading

                    state.userMessage?.let { message ->
                        showSnackbar(message, state.isErrorMessage)
                        viewModel.onMessageShown()
                    }

                    state.shareFile?.let { file ->
                        val mimeType = state.shareMimeType ?: "application/octet-stream"
                        val title = state.shareTitle ?: getString(R.string.share_backup_title)
                        shareGeneratedFile(file, mimeType, title)
                        viewModel.onShareHandled()
                    }

                    if (state.pendingRestorePayload != null && state.pendingValidationSummary != null) {
                        showImportStrategyDialog(state.pendingValidationSummary)
                    }
                }
            }
        }
    }

    private fun shareGeneratedFile(file: File, mimeType: String, title: String) {
        try {
            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, title))
        } catch (e: Exception) {
            showSnackbar("تعذر مشاركة الملف: ${e.message}", true)
        }
    }

    private fun showImportStrategyDialog(summary: BackupSummary) {
        val message = getString(
            R.string.dialog_import_strategy_message,
            summary.transactionsCount,
            summary.goalsCount,
            summary.budgetsCount
        )

        val options = arrayOf(
            getString(R.string.strategy_replace_title) + "\n" + getString(R.string.strategy_replace_desc),
            getString(R.string.strategy_merge_title) + "\n" + getString(R.string.strategy_merge_desc)
        )

        var selectedIndex = 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_import_strategy_title))
            .setMessage(message)
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(getString(R.string.action_confirm)) { _, _ ->
                val strategy = if (selectedIndex == 0) {
                    ImportStrategy.REPLACE_ALL
                } else {
                    ImportStrategy.MERGE
                }
                viewModel.restoreBackup(strategy)
            }
            .setNegativeButton(getString(R.string.action_cancel)) { _, _ ->
                viewModel.dismissStrategyDialog()
            }
            .setOnCancelListener {
                viewModel.dismissStrategyDialog()
            }
            .show()
    }

    private fun showClearDataConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_clear_data_title))
            .setMessage(getString(R.string.dialog_clear_data_message))
            .setPositiveButton(getString(R.string.dialog_clear_data_confirm)) { _, _ ->
                viewModel.clearAllData()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun showSnackbar(message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        if (isError) {
            snackbar.setBackgroundTint(requireContext().getColor(R.color.urwallet_expense))
        }
        snackbar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
