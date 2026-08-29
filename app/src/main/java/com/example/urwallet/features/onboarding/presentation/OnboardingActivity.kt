package com.example.urwallet.features.onboarding.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.urwallet.R
import com.example.urwallet.databinding.ActivityOnboardingBinding
import com.example.urwallet.features.onboarding.presentation.wizard.WizardActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()

    private val totalSlides = 3
    private val dotViews = mutableListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupDots()
        setupButtons()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingPagerAdapter(this)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                val isLastSlide = position == totalSlides - 1
                binding.btnNext.text = if (isLastSlide) {
                    getString(R.string.action_start)
                } else {
                    getString(R.string.action_next)
                }
                binding.btnSkip.visibility = if (isLastSlide) View.GONE else View.VISIBLE
            }
        })
    }

    private fun setupDots() {
        val dotSize = resources.getDimensionPixelSize(R.dimen.spacing_sm)
        val dotMargin = resources.getDimensionPixelSize(R.dimen.spacing_xs)
        repeat(totalSlides) { index ->
            val dot = ImageView(this).apply {
                setImageDrawable(
                    if (index == 0) ContextCompat.getDrawable(context, R.drawable.dot_indicator_selected)
                    else ContextCompat.getDrawable(context, R.drawable.dot_indicator_unselected)
                )
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dotSize
                )
                params.setMargins(dotMargin, 0, dotMargin, 0)
                layoutParams = params
            }
            dotViews.add(dot)
            binding.dotsContainer.addView(dot)
        }
    }

    private fun updateDots(selectedPosition: Int) {
        dotViews.forEachIndexed { index, imageView ->
            imageView.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (index == selectedPosition) R.drawable.dot_indicator_selected
                    else R.drawable.dot_indicator_unselected
                )
            )
        }
    }

    private fun setupButtons() {
        binding.btnSkip.setOnClickListener {
            // Go straight to WizardActivity with skip intent
            navigateToWizard()
        }

        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < totalSlides - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                // Last slide "ابدأ الآن" — go to wizard
                navigateToWizard()
            }
        }
    }

    private fun navigateToWizard() {
        startActivity(Intent(this, WizardActivity::class.java))
        finish()
    }
}
