package com.example.urwallet.features.onboarding.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.urwallet.R
import com.example.urwallet.databinding.FragmentOnboardingSlideBinding

class OnboardingSlide1Fragment : Fragment() {

    private var _binding: FragmentOnboardingSlideBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingSlideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivIllustration.setImageResource(R.drawable.ic_onboarding_tracking)
        binding.tvTitle.setText(R.string.onboarding_slide1_title)
        binding.tvSubtitle.setText(R.string.onboarding_slide1_subtitle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
