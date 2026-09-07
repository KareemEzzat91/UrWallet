package com.example.urwallet.features.goals.presentation.contribute

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentContributeGoalBottomSheetBinding
import com.example.urwallet.features.goals.presentation.GoalsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContributeGoalBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentContributeGoalBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GoalsViewModel by activityViewModels()

    private var goalId: Long = -1L
    private var goalName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goalId = arguments?.getLong(ARG_GOAL_ID) ?: -1L
        goalName = arguments?.getString(ARG_GOAL_NAME).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContributeGoalBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (goalName.isNotBlank()) {
            binding.tvTargetGoalName.text = "الهدف: $goalName"
        }

        setupPresets()
        setupSubmitButton()
    }

    private fun setupPresets() {
        fun setPresetAmount(amount: Double) {
            val currentAmount = binding.etContributeAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
            val newAmount = currentAmount + amount
            binding.etContributeAmount.setText(String.format(java.util.Locale.US, "%.0f", newAmount))
            binding.etContributeAmount.setSelection(binding.etContributeAmount.text?.length ?: 0)
        }

        binding.btnPreset100.setOnClickListener { setPresetAmount(100.0) }
        binding.btnPreset250.setOnClickListener { setPresetAmount(250.0) }
        binding.btnPreset500.setOnClickListener { setPresetAmount(500.0) }
        binding.btnPreset1000.setOnClickListener { setPresetAmount(1000.0) }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitContribution.setOnClickListener {
            val amountStr = binding.etContributeAmount.text?.toString().orEmpty().trim()
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            val note = binding.etContributeNote.text?.toString()?.trim()

            if (amount <= 0.0) {
                binding.etContributeAmount.error = getString(R.string.error_invalid_contribution_amount)
                return@setOnClickListener
            }

            binding.btnSubmitContribution.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val result = viewModel.contributeToGoal(
                    goalId = goalId,
                    amount = amount,
                    note = note
                )

                if (result.isSuccess) {
                    Toast.makeText(requireContext(), R.string.contribution_added_success, Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    binding.btnSubmitContribution.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        result.exceptionOrNull()?.localizedMessage ?: "فشل تسجيل الإيداع",
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
        const val TAG = "ContributeGoalBottomSheetFragment"
        private const val ARG_GOAL_ID = "goalId"
        private const val ARG_GOAL_NAME = "goalName"

        fun newInstance(goalId: Long, goalName: String): ContributeGoalBottomSheetFragment {
            return ContributeGoalBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_GOAL_ID, goalId)
                    putString(ARG_GOAL_NAME, goalName)
                }
            }
        }
    }
}
