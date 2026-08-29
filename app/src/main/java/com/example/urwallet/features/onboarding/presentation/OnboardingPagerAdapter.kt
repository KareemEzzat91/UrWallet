package com.example.urwallet.features.onboarding.presentation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> OnboardingSlide1Fragment()
        1 -> OnboardingSlide2Fragment()
        2 -> OnboardingSlide3Fragment()
        else -> throw IllegalArgumentException("Unknown page: $position")
    }
}
