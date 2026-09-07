package com.example.urwallet.features.goals.presentation.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.urwallet.R
import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.databinding.FragmentAddGoalBottomSheetBinding
import com.example.urwallet.features.goals.presentation.GoalsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddGoalBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddGoalBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalsViewModel by activityViewModels()

    private var selectedDeadlineEpochMs: Long = 0L
    private var selectedPaceMode: GoalPaceMode = GoalPaceMode.BALANCED
    private var selectedIcon: String = "ic_goal_custom"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddGoalBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDefaultDeadline()
        setupDatePicker()
        setupPaceSelector()
        setupIconPicker()
        setupSaveButton()
    }

    private fun initDefaultDeadline() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, 12)
        }
        selectedDeadlineEpochMs = cal.timeInMillis
        updateDeadlineButtonText()
    }

    private fun updateDeadlineButtonText() {
        binding.btnSelectDeadline.text = DateUtils.formatDisplayDate(selectedDeadlineEpochMs)
    }

    private fun setupDatePicker() {
        binding.btnSelectDeadline.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.label_deadline))
                .setCalendarConstraints(constraints)
                .setSelection(selectedDeadlineEpochMs)
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                selectedDeadlineEpochMs = selection
                updateDeadlineButtonText()
            }

            picker.show(childFragmentManager, "GOAL_DEADLINE_PICKER")
        }
    }

    private fun setupPaceSelector() {
        binding.chipGroupPace.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipPaceRelaxed) -> {
                    selectedPaceMode = GoalPaceMode.RELAXED
                    adjustDefaultDeadline(24)
                }
                checkedIds.contains(R.id.chipPaceAggressive) -> {
                    selectedPaceMode = GoalPaceMode.AGGRESSIVE
                    adjustDefaultDeadline(6)
                }
                else -> {
                    selectedPaceMode = GoalPaceMode.BALANCED
                    adjustDefaultDeadline(12)
                }
            }
        }
    }

    private fun adjustDefaultDeadline(months: Int) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, months)
        }
        selectedDeadlineEpochMs = cal.timeInMillis
        updateDeadlineButtonText()
    }

    private fun setupIconPicker() {
        val iconViews = mapOf(
            binding.iconEmergency to "ic_goal_emergency",
            binding.iconTravel to "ic_goal_travel",
            binding.iconHome to "ic_goal_home",
            binding.iconRetirement to "ic_goal_retirement",
            binding.iconCustom to "ic_goal_custom"
        )

        fun updateSelectedIconVisual(selected: String) {
            selectedIcon = selected
            iconViews.forEach { (view, name) ->
                view.setBackgroundResource(
                    if (name == selected) R.drawable.bg_pill_primary else R.drawable.bg_icon_circle
                )
            }
        }

        iconViews.forEach { (view, name) ->
            view.setOnClickListener {
                updateSelectedIconVisual(name)
            }
        }

        updateSelectedIconVisual("ic_goal_custom")
    }

    private fun setupSaveButton() {
        binding.btnSaveGoal.setOnClickListener {
            val name = binding.etGoalName.text?.toString().orEmpty().trim()
            val amountStr = binding.etTargetAmount.text?.toString().orEmpty().trim()
            val targetAmount = amountStr.toDoubleOrNull() ?: 0.0

            if (name.isBlank()) {
                binding.etGoalName.error = getString(R.string.error_empty_goal_name)
                return@setOnClickListener
            }
            if (targetAmount <= 0.0) {
                binding.etTargetAmount.error = getString(R.string.error_invalid_goal_target)
                return@setOnClickListener
            }
            if (selectedDeadlineEpochMs <= System.currentTimeMillis()) {
                Toast.makeText(requireContext(), R.string.error_invalid_deadline, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSaveGoal.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val result = viewModel.addGoal(
                    name = name,
                    targetAmount = targetAmount,
                    deadline = selectedDeadlineEpochMs,
                    paceMode = selectedPaceMode,
                    icon = selectedIcon
                )

                if (result.isSuccess) {
                    Toast.makeText(requireContext(), R.string.goal_saved_success, Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    binding.btnSaveGoal.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        result.exceptionOrNull()?.localizedMessage ?: "فشل حفظ الهدف",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddGoalBottomSheetFragment"
        fun newInstance() = AddGoalBottomSheetFragment()
    }
}
