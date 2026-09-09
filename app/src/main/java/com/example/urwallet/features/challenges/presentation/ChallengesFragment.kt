package com.example.urwallet.features.challenges.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentChallengesBinding
import com.example.urwallet.features.challenges.presentation.adapter.ActiveChallengesAdapter
import com.example.urwallet.features.challenges.presentation.adapter.ChallengePresetsAdapter
import com.example.urwallet.features.challenges.presentation.adapter.CompletedChallengesAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChallengesFragment : Fragment() {

    private var _binding: FragmentChallengesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChallengesViewModel by viewModels()

    private lateinit var activeAdapter: ActiveChallengesAdapter
    private lateinit var presetsAdapter: ChallengePresetsAdapter
    private lateinit var completedAdapter: CompletedChallengesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupAdapters()
        observeState()
        observeEvents()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAdapters() {
        activeAdapter = ActiveChallengesAdapter { challengeId ->
            navigateToDetail(challengeId)
        }
        binding.rvActiveChallenges.adapter = activeAdapter

        presetsAdapter = ChallengePresetsAdapter { preset ->
            viewModel.joinPreset(preset)
        }
        binding.rvChallengePresets.adapter = presetsAdapter

        completedAdapter = CompletedChallengesAdapter { challengeId ->
            navigateToDetail(challengeId)
        }
        binding.rvCompletedChallenges.adapter = completedAdapter
    }

    private fun navigateToDetail(challengeId: Long) {
        val bundle = bundleOf("challengeId" to challengeId)
        findNavController().navigate(R.id.action_challengesFragment_to_challengeDetailFragment, bundle)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.challengesUiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvents.collect { event ->
                    when (event) {
                        is ChallengesUiEvent.ChallengeJoined -> {
                            Snackbar.make(binding.root, "تم الانضمام للتحدي بنجاح! 🔥", Snackbar.LENGTH_SHORT).show()
                            navigateToDetail(event.challengeId)
                        }
                        is ChallengesUiEvent.ShowMessage -> {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: ChallengesUiState) {
        when (state) {
            is ChallengesUiState.Loading -> {
                binding.progressLoading.isVisible = true
                binding.scrollViewContent.isVisible = false
                binding.layoutError.isVisible = false
            }
            is ChallengesUiState.Success -> {
                binding.progressLoading.isVisible = false
                binding.scrollViewContent.isVisible = true
                binding.layoutError.isVisible = false

                val hasActive = state.activeChallenges.isNotEmpty()
                binding.cardNoActivePrompt.isVisible = !hasActive
                binding.rvActiveChallenges.isVisible = hasActive
                activeAdapter.submitList(state.activeChallenges)

                presetsAdapter.submitList(state.presets)

                val hasCompleted = state.completedChallenges.isNotEmpty()
                binding.rvCompletedChallenges.isVisible = hasCompleted
                binding.tvNoCompletedYet.isVisible = !hasCompleted
                completedAdapter.submitList(state.completedChallenges)
            }
            is ChallengesUiState.Empty -> {
                binding.progressLoading.isVisible = false
                binding.scrollViewContent.isVisible = true
                binding.layoutError.isVisible = false

                binding.cardNoActivePrompt.isVisible = true
                binding.rvActiveChallenges.isVisible = false
                activeAdapter.submitList(emptyList())

                presetsAdapter.submitList(state.presets)

                binding.rvCompletedChallenges.isVisible = false
                binding.tvNoCompletedYet.isVisible = true
                completedAdapter.submitList(emptyList())
            }
            is ChallengesUiState.Error -> {
                binding.progressLoading.isVisible = false
                binding.scrollViewContent.isVisible = false
                binding.layoutError.isVisible = true
                binding.tvErrorMessage.text = state.message
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
