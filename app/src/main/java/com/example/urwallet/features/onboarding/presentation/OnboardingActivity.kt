package com.example.urwallet.features.onboarding.presentation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.viewpager2.widget.ViewPager2
import com.example.urwallet.R
import com.example.urwallet.databinding.ActivityOnboardingBinding
import com.example.urwallet.features.onboarding.presentation.wizard.WizardActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

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

        setupInsets()
        setupViewPager()
        setupDots()
        setupButtons()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.btnSkip.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBars.top + resources.getDimensionPixelSize(R.dimen.spacing_md)
            }
            binding.bottomBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBars.bottom + resources.getDimensionPixelSize(R.dimen.spacing_md)
            }
            insets
        }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingPagerAdapter(this)

        // Fluid Parallax and Scale PageTransformer
        binding.viewPager.setPageTransformer { page, position ->
            val absPos = abs(position)
            if (position < -1) {
                page.alpha = 0f
            } else if (position <= 1) {
                // Subtle scale between 0.90 and 1.0
                val scale = 0.90f.coerceAtLeast(1 - absPos * 0.10f)
                page.scaleX = scale
                page.scaleY = scale
                page.alpha = 0.4f.coerceAtLeast(1 - absPos * 0.6f)

                // Parallax shift for illustration container
                val illustration = page.findViewById<View>(R.id.illustration_container)
                illustration?.translationX = -position * (page.width * 0.20f)
            } else {
                page.alpha = 0f
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                val isLastSlide = position == totalSlides - 1
                binding.btnNext.text = if (isLastSlide) {
                    getString(R.string.action_start)
                } else {
                    getString(R.string.action_next)
                }

                if (isLastSlide) {
                    binding.btnSkip.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction { binding.btnSkip.visibility = View.GONE }
                        .start()
                } else {
                    binding.btnSkip.visibility = View.VISIBLE
                    binding.btnSkip.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
            }
        })
    }

    private fun setupDots() {
        val dotHeight = resources.getDimensionPixelSize(R.dimen.spacing_sm)
        val dotMargin = resources.getDimensionPixelSize(R.dimen.spacing_xs)
        repeat(totalSlides) { index ->
            val dot = ImageView(this).apply {
                setImageDrawable(
                    if (index == 0) ContextCompat.getDrawable(context, R.drawable.dot_indicator_selected)
                    else ContextCompat.getDrawable(context, R.drawable.dot_indicator_unselected)
                )
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dotHeight
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
            val isSelected = index == selectedPosition
            imageView.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (isSelected) R.drawable.dot_indicator_selected
                    else R.drawable.dot_indicator_unselected
                )
            )
            imageView.animate()
                .scaleX(if (isSelected) 1.08f else 1.0f)
                .scaleY(if (isSelected) 1.08f else 1.0f)
                .setDuration(180)
                .start()
        }
    }

    private fun setupButtons() {
        binding.btnSkip.setOnClickListener {
            navigateToWizard()
        }

        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < totalSlides - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                navigateToWizard()
            }
        }
    }

    private fun navigateToWizard() {
        startActivity(Intent(this, WizardActivity::class.java))
        finish()
    }
}
