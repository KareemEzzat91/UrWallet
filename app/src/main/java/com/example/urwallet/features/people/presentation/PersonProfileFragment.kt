package com.example.urwallet.features.people.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.urwallet.R
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.FragmentPersonProfileBinding
import com.example.urwallet.features.people.domain.model.Person
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PersonProfileFragment : Fragment() {

    private var _binding: FragmentPersonProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PersonProfileViewModel by viewModels()

    private lateinit var obligationsAdapter: PersonObligationsAdapter
    private lateinit var transactionsAdapter: PersonTransactionsAdapter

    private var currentPerson: Person? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupAdapters()
        setupTabs()
        setupActions()
        observeState()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEditPerson.setOnClickListener {
            currentPerson?.let { person ->
                AddEditPersonBottomSheetFragment.newInstance(person).show(
                    childFragmentManager,
                    AddEditPersonBottomSheetFragment.TAG
                )
            }
        }

        binding.btnDeletePerson.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupAdapters() {
        obligationsAdapter = PersonObligationsAdapter { obligation ->
            val sheet = SettleObligationBottomSheetFragment.newInstance(obligation)
            sheet.onSettleConfirmed = { id, amount, note, createTx ->
                viewModel.settleObligation(id, amount, note, createTx)
            }
            sheet.show(childFragmentManager, SettleObligationBottomSheetFragment.TAG)
        }
        binding.rvProfileObligations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = obligationsAdapter
        }

        transactionsAdapter = PersonTransactionsAdapter()
        binding.rvProfileTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionsAdapter
        }
    }

    private fun setupTabs() {
        binding.cgProfileTabs.setOnCheckedStateChangeListener { _, checkedIds ->
            val isObligations = checkedIds.contains(R.id.chipObligations)
            binding.rvProfileObligations.isVisible = isObligations && obligationsAdapter.itemCount > 0
            binding.layoutEmptyProfileObligations.isVisible = isObligations && obligationsAdapter.itemCount == 0

            binding.rvProfileTransactions.isVisible = !isObligations && transactionsAdapter.itemCount > 0
            binding.layoutEmptyProfileTransactions.isVisible = !isObligations && transactionsAdapter.itemCount == 0
        }
    }

    private fun setupActions() {
        binding.btnAddObligation.setOnClickListener {
            val sheet = AddObligationBottomSheetFragment.newInstance()
            sheet.onSaveObligation = { title, amount, direction, dueDate, notes ->
                viewModel.addObligation(title, amount, direction, dueDate, notes)
            }
            sheet.show(childFragmentManager, AddObligationBottomSheetFragment.TAG)
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_person_title)
            .setMessage(R.string.dialog_delete_person_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deletePerson()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.person.collectLatest { person ->
                        currentPerson = person
                        if (person != null) {
                            binding.tvProfileAvatarInitial.text =
                                person.name.trim().firstOrNull()?.uppercase() ?: "؟"
                            binding.tvProfilePersonName.text = person.name

                            val details = listOfNotNull(
                                person.phoneNumber?.takeIf { it.isNotBlank() },
                                person.notes?.takeIf { it.isNotBlank() }
                            )
                            binding.tvProfileSubtitle.text = if (details.isNotEmpty()) {
                                details.joinToString(" • ")
                            } else {
                                getString(R.string.cat_other)
                            }

                            if (!person.notes.isNullOrBlank()) {
                                binding.tvProfileNotes.text = "ملاحظات: ${person.notes}"
                                binding.tvProfileNotes.isVisible = true
                            } else {
                                binding.tvProfileNotes.isVisible = false
                            }
                        }
                    }
                }

                launch {
                    viewModel.summary.collectLatest { summary ->
                        if (summary != null) {
                            // Debts
                            binding.tvProfileOwedToMe.text = Formatters.formatCurrency(summary.totalOwedToMe)
                            binding.tvProfileIOwe.text = Formatters.formatCurrency(summary.totalIOwe)

                            val netDebt = summary.netObligationBalance
                            val netDebtFormatted = Formatters.formatCurrency(kotlin.math.abs(netDebt))
                            binding.tvProfileNetDebt.text = when {
                                netDebt > 0.001 -> "+ $netDebtFormatted"
                                netDebt < -0.001 -> "- $netDebtFormatted"
                                else -> "0 ج.م"
                            }

                            // Flow
                            binding.tvProfileTotalPaid.text = Formatters.formatCurrency(summary.totalSent)
                            binding.tvProfileTotalReceived.text = Formatters.formatCurrency(summary.totalReceived)

                            val netFlow = summary.netTransactionFlow
                            val netFlowFormatted = Formatters.formatCurrency(kotlin.math.abs(netFlow))
                            binding.tvProfileNetFlow.text = when {
                                netFlow > 0.001 -> "+ $netFlowFormatted"
                                netFlow < -0.001 -> "- $netFlowFormatted"
                                else -> "0 ج.م"
                            }
                        }
                    }
                }

                launch {
                    viewModel.obligations.collectLatest { list ->
                        obligationsAdapter.submitList(list)
                        if (binding.chipObligations.isChecked) {
                            binding.rvProfileObligations.isVisible = list.isNotEmpty()
                            binding.layoutEmptyProfileObligations.isVisible = list.isEmpty()
                        }
                    }
                }

                launch {
                    viewModel.transactions.collectLatest { list ->
                        transactionsAdapter.submitList(list)
                        if (binding.chipTransactions.isChecked) {
                            binding.rvProfileTransactions.isVisible = list.isNotEmpty()
                            binding.layoutEmptyProfileTransactions.isVisible = list.isEmpty()
                        }
                    }
                }

                launch {
                    viewModel.uiEvent.collectLatest { event ->
                        when (event) {
                            is ProfileUiEvent.ShowMessage -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            is ProfileUiEvent.ShowError -> {
                                Toast.makeText(requireContext(), event.error, Toast.LENGTH_LONG).show()
                            }
                            is ProfileUiEvent.PersonDeleted -> {
                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
