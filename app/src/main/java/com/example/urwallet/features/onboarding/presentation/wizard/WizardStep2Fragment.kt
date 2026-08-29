package com.example.urwallet.features.onboarding.presentation.wizard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.urwallet.databinding.FragmentWizardStep2Binding
import com.example.urwallet.features.onboarding.presentation.OnboardingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WizardStep2Fragment : Fragment() {

    private var _binding: FragmentWizardStep2Binding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by activityViewModels()

    private var isUpdatingFromSlider = false
    private var isUpdatingFromText = false

    // Slider maps 0..100 → 500..100000
    private val minAmount = 500.0
    private val maxAmount = 100_000.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWizardStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentAmount = viewModel.wizardState.value.targetAmount
        binding.etAmount.setText(currentAmount.toInt().toString())
        binding.sliderAmount.progress = amountToProgress(currentAmount)

        setupAmountInput()
        setupSlider()
        setupPresetChips()
    }

    private fun setupAmountInput() {
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingFromSlider) return
                val text = s?.toString() ?: return
                val amount = text.toDoubleOrNull() ?: return
                if (amount > 0) {
                    viewModel.setTargetAmount(amount)
                    isUpdatingFromText = true
                    binding.sliderAmount.progress = amountToProgress(amount)
                    isUpdatingFromText = false
                }
            }
        })
    }

    private fun setupSlider() {
        binding.sliderAmount.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isUpdatingFromText) return
                val amount = progressToAmount(progress)
                viewModel.setTargetAmount(amount)
                isUpdatingFromSlider = true
                binding.etAmount.setText(amount.toInt().toString())
                isUpdatingFromSlider = false
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupPresetChips() {
        binding.chip1000.setOnClickListener { setAmount(1000.0) }
        binding.chip5000.setOnClickListener { setAmount(5000.0) }
        binding.chip10000.setOnClickListener { setAmount(10000.0) }
        binding.chip20000.setOnClickListener { setAmount(20000.0) }
    }

    private fun setAmount(amount: Double) {
        viewModel.setTargetAmount(amount)
        binding.etAmount.setText(amount.toInt().toString())
        binding.sliderAmount.progress = amountToProgress(amount)
    }

    private fun amountToProgress(amount: Double): Int {
        return ((amount - minAmount) / (maxAmount - minAmount) * 100).toInt().coerceIn(0, 100)
    }

    private fun progressToAmount(progress: Int): Double {
        return minAmount + (progress.toDouble() / 100.0) * (maxAmount - minAmount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
