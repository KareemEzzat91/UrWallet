package com.example.urwallet.features.more.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentMoreBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardBudgetsNav.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_budgetsFragment)
        }

        binding.cardAnalyticsNav.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_analyticsFragment)
        }

        binding.cardHabitsNav.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_habitsFragment)
        }

        binding.cardChallengesNav.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_challengesFragment)
        }

        binding.cardRecurringNav.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_recurringFragment)
        }

        binding.cardNotificationsNav.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_notificationSettingsFragment)
        }

        binding.cardSecurityNav.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_securitySettingsFragment)
        }

        binding.cardDataManagementNav.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_dataManagementFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
