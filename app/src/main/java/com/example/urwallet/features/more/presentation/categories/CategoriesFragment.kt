package com.example.urwallet.features.more.presentation.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.urwallet.R
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.databinding.FragmentCategoriesBinding
import com.example.urwallet.features.transactions.domain.model.Category
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoriesViewModel by viewModels()
    private lateinit var categoriesAdapter: CategoriesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupTypeToggle()
        setupRecyclerView()
        setupFab()
        observeState()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupTypeToggle() {
        binding.toggleGroupType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val selectedType = if (checkedId == R.id.btnTabIncome) {
                    CategoryType.INCOME
                } else {
                    CategoryType.EXPENSE
                }
                viewModel.setTypeFilter(selectedType)
            }
        }
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter(
            onEditClick = { category ->
                AddEditCategoryBottomSheetFragment.newInstance(category)
                    .show(childFragmentManager, AddEditCategoryBottomSheetFragment.TAG)
            },
            onDeleteClick = { category ->
                showDeleteConfirmationDialog(category)
            }
        )
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoriesAdapter
        }
    }

    private fun showDeleteConfirmationDialog(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.category_delete_confirm_title)
            .setMessage(R.string.category_delete_confirm_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteCategory(category)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun setupFab() {
        binding.fabAddCategory.setOnClickListener {
            val currentType = viewModel.uiState.value.selectedType
            AddEditCategoryBottomSheetFragment.newInstance(null, currentType)
                .show(childFragmentManager, AddEditCategoryBottomSheetFragment.TAG)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    categoriesAdapter.submitList(state.categories)
                    binding.layoutEmptyState.isVisible = !state.isLoading && state.categories.isEmpty()

                    state.userMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearMessages()
                    }

                    state.errorMessage?.let { error ->
                        Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                        viewModel.clearMessages()
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
