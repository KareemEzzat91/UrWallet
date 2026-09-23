package com.example.urwallet.features.more.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.urwallet.R
import com.example.urwallet.core.common.Constants
import com.example.urwallet.core.common.Formatters
import com.example.urwallet.core.datastore.AppPreferences
import com.example.urwallet.databinding.FragmentMoreBinding
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var financialEventRepository: FinancialEventRepository

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

        binding.cardFinancialInboxNav.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_financialInboxFragment)
        }

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

        binding.cardCategoriesNav.setOnClickListener {
            findNavController().navigate(R.id.action_moreFragment_to_categoriesFragment)
        }

        binding.cardCurrencyNav.setOnClickListener {
            showCurrencySelectionDialog()
        }

        observeCurrency()
        observeFinancialInboxBadge()
    }

    private fun observeFinancialInboxBadge() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                financialEventRepository.getPendingCount().collect { count ->
                    if (count > 0) {
                        binding.tvInboxPendingBadge.isVisible = true
                        binding.tvInboxPendingBadge.text = count.toString()
                    } else {
                        binding.tvInboxPendingBadge.isVisible = false
                    }
                }
            }
        }
    }

    private fun observeCurrency() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appPreferences.currencySymbol.collect { symbol ->
                    binding.tvCurrencySymbolIcon.text = symbol
                    val currency = Constants.SUPPORTED_CURRENCIES.find { it.symbol == symbol }
                    binding.tvCurrentCurrency.text = currency?.displayNameAr ?: symbol
                }
            }
        }
    }

    private fun showCurrencySelectionDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentSymbol = appPreferences.currencySymbol.first()
            val items = Constants.SUPPORTED_CURRENCIES.map { it.displayNameAr }.toTypedArray()
            val currentIndex = Constants.SUPPORTED_CURRENCIES.indexOfFirst { it.symbol == currentSymbol }.let {
                if (it >= 0) it else 0
            }

            var selectedIndex = currentIndex
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_select_currency_title)
                .setSingleChoiceItems(items, currentIndex) { _, which ->
                    selectedIndex = which
                }
                .setPositiveButton(R.string.security_action_confirm) { dialog, _ ->
                    val selectedCurrency = Constants.SUPPORTED_CURRENCIES[selectedIndex]
                    viewLifecycleOwner.lifecycleScope.launch {
                        appPreferences.setCurrencySymbol(selectedCurrency.symbol)
                        appPreferences.setCurrencyCode(selectedCurrency.code)
                        Formatters.activeCurrencySymbol = selectedCurrency.symbol
                        Snackbar.make(
                            binding.root,
                            R.string.currency_updated_success,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.security_action_cancel, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

