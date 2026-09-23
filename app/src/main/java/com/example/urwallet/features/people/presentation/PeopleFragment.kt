package com.example.urwallet.features.people.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.urwallet.R
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.FragmentPeopleBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PeopleFragment : Fragment() {

    private var _binding: FragmentPeopleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PeopleViewModel by activityViewModels()
    private lateinit var peopleAdapter: PeopleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeopleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        observeState()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAddPerson.setOnClickListener {
            AddEditPersonBottomSheetFragment.newInstance().show(
                childFragmentManager,
                AddEditPersonBottomSheetFragment.TAG
            )
        }

        binding.btnEmptyAddPerson.setOnClickListener {
            AddEditPersonBottomSheetFragment.newInstance().show(
                childFragmentManager,
                AddEditPersonBottomSheetFragment.TAG
            )
        }
    }
    private fun setupRecyclerView() {
        peopleAdapter = PeopleAdapter { summary ->
            val bundle = Bundle().apply {
                putLong("personId", summary.person.id)
            }
            findNavController().navigate(R.id.action_peopleFragment_to_personProfileFragment, bundle)
        }
        binding.rvPeople.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = peopleAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchPeople.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.peopleSummaries.collectLatest { list ->
                        peopleAdapter.submitList(list)
                        binding.layoutEmptyPeople.isVisible = list.isEmpty()
                        binding.rvPeople.isVisible = list.isNotEmpty()
                    }
                }

                launch {
                    viewModel.analytics.collectLatest { analytics ->
                        if (analytics != null) {
                            binding.tvTotalOwedToMe.text = Formatters.formatCurrency(analytics.totalOwedToMe)
                            binding.tvTotalIOwe.text = Formatters.formatCurrency(analytics.totalIOwe)

                            val net = analytics.netObligationBalance
                            val netFormatted = Formatters.formatCurrency(kotlin.math.abs(net))
                            binding.tvNetDebtBalance.text = when {
                                net > 0.001 -> "+ $netFormatted"
                                net < -0.001 -> "- $netFormatted"
                                else -> "0 ج.م"
                            }
                        }
                    }
                }

                launch {
                    viewModel.uiEvent.collectLatest { event ->
                        when (event) {
                            is PeopleUiEvent.ShowMessage -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            is PeopleUiEvent.ShowError -> {
                                Toast.makeText(requireContext(), event.error, Toast.LENGTH_LONG).show()
                            }
                            is PeopleUiEvent.PersonCreated -> {
                                // Handled via toast and list reload
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
