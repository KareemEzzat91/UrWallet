package com.example.urwallet.features.onboarding.presentation.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.urwallet.databinding.FragmentWizardStep5Binding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WizardStep5Fragment : Fragment() {

    private var _binding: FragmentWizardStep5Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWizardStep5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
