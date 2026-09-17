package com.example.urwallet.features.more.presentation.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.urwallet.R
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.databinding.BottomSheetAddEditCategoryBinding
import com.example.urwallet.features.transactions.domain.model.Category
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddEditCategoryBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddEditCategoryBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel from parent fragment
    private val viewModel: CategoriesViewModel by viewModels({ requireParentFragment() })

    private var categoryId: Long = 0L
    private var selectedType: CategoryType = CategoryType.EXPENSE
    private var selectedIcon: String = "ic_other"
    private var selectedColor: String = "#78909C"

    private val availableIcons = listOf(
        "ic_food", "ic_transport", "ic_shopping", "ic_bills",
        "ic_entertainment", "ic_health", "ic_education", "ic_other",
        "ic_salary", "ic_freelance", "ic_bonus"
    )

    private val availableColors = listOf(
        "#FF7043", "#42A5F5", "#AB47BC", "#FFA726",
        "#26A69A", "#EF5350", "#5C6BC0", "#00732C",
        "#00897B", "#8E24AA", "#43A047", "#78909C"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getLong(ARG_ID, 0L)
            selectedType = CategoryType.valueOf(it.getString(ARG_TYPE, CategoryType.EXPENSE.name))
            selectedIcon = it.getString(ARG_ICON, "ic_other")
            selectedColor = it.getString(ARG_COLOR, "#78909C")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupPickers()
    }

    private fun setupViews() {
        val isEdit = categoryId > 0
        binding.tvSheetTitle.setText(
            if (isEdit) R.string.category_edit_title else R.string.category_add_title
        )

        arguments?.getString(ARG_NAME)?.let {
            binding.etCategoryName.setText(it)
        }

        if (selectedType == CategoryType.INCOME) {
            binding.toggleCategoryType.check(R.id.btnTypeIncome)
        } else {
            binding.toggleCategoryType.check(R.id.btnTypeExpense)
        }

        binding.toggleCategoryType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedType = if (checkedId == R.id.btnTypeIncome) CategoryType.INCOME else CategoryType.EXPENSE
            }
        }

        binding.btnSaveCategory.setOnClickListener {
            val name = binding.etCategoryName.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                binding.tilCategoryName.error = getString(R.string.category_name_empty_error)
                return@setOnClickListener
            }
            binding.tilCategoryName.error = null

            viewModel.saveCategory(
                id = categoryId,
                name = name,
                type = selectedType,
                icon = selectedIcon,
                color = selectedColor
            )
            dismiss()
        }
    }

    private fun setupPickers() {
        val iconAdapter = IconPickerAdapter(availableIcons, selectedIcon) { icon ->
            selectedIcon = icon
        }
        binding.rvIcons.adapter = iconAdapter

        val colorAdapter = ColorPickerAdapter(availableColors, selectedColor) { color ->
            selectedColor = color
        }
        binding.rvColors.adapter = colorAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddEditCategoryBottomSheet"
        private const val ARG_ID = "arg_id"
        private const val ARG_NAME = "arg_name"
        private const val ARG_TYPE = "arg_type"
        private const val ARG_ICON = "arg_icon"
        private const val ARG_COLOR = "arg_color"

        fun newInstance(category: Category? = null, defaultType: CategoryType = CategoryType.EXPENSE): AddEditCategoryBottomSheetFragment {
            return AddEditCategoryBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    if (category != null) {
                        putLong(ARG_ID, category.id)
                        putString(ARG_NAME, category.name)
                        putString(ARG_TYPE, category.type.name)
                        putString(ARG_ICON, category.icon)
                        putString(ARG_COLOR, category.color)
                    } else {
                        putString(ARG_TYPE, defaultType.name)
                    }
                }
            }
        }
    }
}
