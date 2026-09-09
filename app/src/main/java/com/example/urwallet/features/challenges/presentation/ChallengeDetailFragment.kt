package com.example.urwallet.features.challenges.presentation

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
import com.example.urwallet.R
import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.databinding.FragmentChallengeDetailBinding
import com.example.urwallet.features.challenges.domain.model.ChallengeProgress
import com.example.urwallet.features.challenges.presentation.adapter.ChallengeDaysAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChallengeDetailFragment : Fragment() {

    private var _binding: FragmentChallengeDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChallengeDetailViewModel by viewModels()
    private lateinit var timelineAdapter: ChallengeDaysAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTimelineList()
        setupAbandonButton()
        observeState()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupTimelineList() {
        timelineAdapter = ChallengeDaysAdapter()
        binding.rvDailyTimeline.adapter = timelineAdapter
    }

    private fun setupAbandonButton() {
        binding.btnAbandonChallenge.setOnClickListener {
            showAbandonConfirmationDialog()
        }
    }

    private fun showAbandonConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_abandon_title)
            .setMessage(R.string.dialog_abandon_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                viewModel.abandonChallenge()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: ChallengeDetailUiState) {
        when (state) {
            is ChallengeDetailUiState.Loading -> {
                binding.progressLoading.isVisible = true
                binding.scrollViewContent.isVisible = false
            }
            is ChallengeDetailUiState.Success -> {
                binding.progressLoading.isVisible = false
                binding.scrollViewContent.isVisible = true
                bindChallenge(state.progress)
            }
            is ChallengeDetailUiState.Error -> {
                binding.progressLoading.isVisible = false
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            is ChallengeDetailUiState.Deleted -> {
                binding.progressLoading.isVisible = false
                findNavController().navigateUp()
            }
        }
    }

    private fun bindChallenge(progress: ChallengeProgress) {
        val challenge = progress.challenge

        binding.tvChallengeTitle.text = challenge.title
        binding.tvRulesDescription.text = challenge.description

        // Type Chip
        val typeText = when (challenge.type) {
            ChallengeType.NO_SPENDING -> getString(R.string.challenge_type_no_spending)
            ChallengeType.SAVE_AMOUNT -> getString(R.string.challenge_type_save_amount)
            ChallengeType.REDUCE_CATEGORY -> getString(R.string.challenge_type_reduce_category)
        }
        binding.chipType.text = typeText

        // Streak badge
        binding.tvStreakCount.text = getString(R.string.streak_days_format, progress.currentStreak)

        // Progress Bar
        val pctInt = progress.progressPercentage.toInt()
        binding.progressIndicator.progress = pctInt
        binding.tvProgressPercent.text = "${pctInt}%"

        // Progress description
        binding.tvProgressDesc.text = when (challenge.type) {
            ChallengeType.NO_SPENDING -> {
                val target = progress.targetDays ?: 7
                getString(R.string.challenge_progress_days_format, progress.completedDays, target)
            }
            ChallengeType.SAVE_AMOUNT -> {
                val target = progress.targetAmount ?: 0.0
                val currentFormatted = Formatters.formatCurrency(progress.currentProgress)
                val targetFormatted = Formatters.formatCurrency(target)
                getString(R.string.challenge_progress_amount_format, currentFormatted, targetFormatted)
            }
            ChallengeType.REDUCE_CATEGORY -> {
                val limit = progress.targetAmount ?: 0.0
                val spentFormatted = Formatters.formatCurrency(progress.currentProgress)
                val limitFormatted = Formatters.formatCurrency(limit)
                getString(R.string.challenge_progress_limit_format, spentFormatted, limitFormatted)
            }
        }

        // Stats Row
        binding.tvCompletedDaysValue.text = progress.completedDays.toString()
        binding.tvRemainingDaysValue.text = progress.remainingDays.toString()
        binding.tvLongestStreakValue.text = progress.longestStreak.toString()

        // Daily Timeline
        if (progress.dailyStatuses.isNotEmpty()) {
            binding.tvTimelineHeader.isVisible = true
            binding.rvDailyTimeline.isVisible = true
            timelineAdapter.submitList(progress.dailyStatuses)
        } else {
            binding.tvTimelineHeader.isVisible = false
            binding.rvDailyTimeline.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
