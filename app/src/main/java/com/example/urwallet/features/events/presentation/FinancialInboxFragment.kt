package com.example.urwallet.features.events.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentFinancialInboxBinding
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FinancialInboxFragment : Fragment() {

    private var _binding: FragmentFinancialInboxBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FinancialInboxViewModel by activityViewModels()
    private lateinit var adapter: FinancialInboxAdapter

    private var currentFilterTab = FilterTab.PENDING

    private enum class FilterTab {
        PENDING, CONFIRMED, DISMISSED
    }

    private val requestSmsPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        val readGranted = permissions[Manifest.permission.READ_SMS] == true

        if (receiveGranted || readGranted) {
            viewModel.setSmsDetectionEnabled(true)
            binding.switchSmsDetection.isChecked = true
            binding.layoutPermissionBanner.isVisible = false
            Toast.makeText(requireContext(), "تم تفعيل كشف المعاملات بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            binding.switchSmsDetection.isChecked = false
            binding.layoutPermissionBanner.isVisible = true
            Toast.makeText(requireContext(), R.string.permission_sms_rationale, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinancialInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupFilterTabs()
        setupDetectionToggle()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnScanSms.setOnClickListener {
            val hasReadPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasReadPermission) {
                viewModel.scanRecentSms()
            } else {
                requestSmsPermissionsLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = FinancialInboxAdapter(
            onConfirmClick = { event ->
                showReviewBottomSheet(event)
            },
            onEditClick = { event ->
                showReviewBottomSheet(event)
            },
            onDismissClick = { event ->
                showDismissConfirmationDialog(event)
            }
        )
        binding.rvEvents.adapter = adapter
    }

    private fun setupFilterTabs() {
        binding.cgFilterTabs.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipPending) -> {
                    currentFilterTab = FilterTab.PENDING
                    updateDisplayedList()
                }
                checkedIds.contains(R.id.chipConfirmed) -> {
                    currentFilterTab = FilterTab.CONFIRMED
                    updateDisplayedList()
                }
                checkedIds.contains(R.id.chipDismissed) -> {
                    currentFilterTab = FilterTab.DISMISSED
                    updateDisplayedList()
                }
            }
        }
    }

    private fun setupDetectionToggle() {
        binding.switchSmsDetection.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val hasReceive = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                val hasRead = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

                if (!hasReceive || !hasRead) {
                    requestSmsPermissionsLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
                } else {
                    viewModel.setSmsDetectionEnabled(true)
                    binding.layoutPermissionBanner.isVisible = false
                }
            } else {
                viewModel.setSmsDetectionEnabled(false)
            }
        }

        binding.btnGrantPermission.setOnClickListener {
            requestSmsPermissionsLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isSmsDetectionEnabled.collect { enabled ->
                        binding.switchSmsDetection.isChecked = enabled
                        val hasSmsPermission = ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.RECEIVE_SMS
                        ) == PackageManager.PERMISSION_GRANTED
                        binding.layoutPermissionBanner.isVisible = enabled && !hasSmsPermission
                    }
                }

                launch {
                    viewModel.pendingCount.collect { count ->
                        binding.chipPending.text = getString(R.string.inbox_tab_pending, count)
                    }
                }

                launch {
                    viewModel.isScanning.collect { isScanning ->
                        binding.pbScanProgress.isVisible = isScanning
                        binding.btnScanSms.isVisible = !isScanning
                    }
                }

                launch {
                    viewModel.pendingEvents.collect {
                        if (currentFilterTab == FilterTab.PENDING) updateDisplayedList()
                    }
                }

                launch {
                    viewModel.confirmedEvents.collect {
                        if (currentFilterTab == FilterTab.CONFIRMED) updateDisplayedList()
                    }
                }

                launch {
                    viewModel.dismissedEvents.collect {
                        if (currentFilterTab == FilterTab.DISMISSED) updateDisplayedList()
                    }
                }

                launch {
                    viewModel.uiEvents.collect { event ->
                        when (event) {
                            is FinancialInboxUiEvent.ShowMessage -> {
                                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                            }
                            is FinancialInboxUiEvent.ScanCompleted -> {
                                val msg = if (event.newCount > 0) {
                                    getString(R.string.scan_sms_success, event.newCount)
                                } else {
                                    getString(R.string.scan_sms_no_new)
                                }
                                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateDisplayedList() {
        val list = when (currentFilterTab) {
            FilterTab.PENDING -> viewModel.pendingEvents.value
            FilterTab.CONFIRMED -> viewModel.confirmedEvents.value
            FilterTab.DISMISSED -> viewModel.dismissedEvents.value
        }

        adapter.submitList(list)

        val isEmpty = list.isEmpty()
        binding.rvEvents.isVisible = !isEmpty
        binding.layoutEmptyState.isVisible = isEmpty

        if (isEmpty) {
            when (currentFilterTab) {
                FilterTab.PENDING -> {
                    binding.tvEmptyTitle.text = getString(R.string.inbox_empty_pending_title)
                    binding.tvEmptyDesc.text = getString(R.string.inbox_empty_pending_desc)
                }
                FilterTab.CONFIRMED -> {
                    binding.tvEmptyTitle.text = getString(R.string.inbox_empty_confirmed_title)
                    binding.tvEmptyDesc.text = getString(R.string.inbox_empty_confirmed_desc)
                }
                FilterTab.DISMISSED -> {
                    binding.tvEmptyTitle.text = getString(R.string.inbox_empty_dismissed_title)
                    binding.tvEmptyDesc.text = getString(R.string.inbox_empty_dismissed_desc)
                }
            }
        }
    }

    private fun showReviewBottomSheet(event: FinancialEvent) {
        val sheet = FinancialEventReviewBottomSheetFragment.newInstance(event)
        sheet.show(childFragmentManager, FinancialEventReviewBottomSheetFragment.TAG)
    }

    private fun showDismissConfirmationDialog(event: FinancialEvent) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_dismiss_confirm_title)
            .setMessage(R.string.dialog_dismiss_confirm_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_dismiss_event) { _, _ ->
                viewModel.dismissEvent(event.id)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
