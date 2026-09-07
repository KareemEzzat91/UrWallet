package com.example.urwallet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.urwallet.databinding.ActivityMainBinding
import com.example.urwallet.features.transactions.presentation.AddTransactionBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupNavigation()
        setupFab()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainCoordinator) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Connect BottomNavigationView to NavController
        NavigationUI.setupWithNavController(binding.bottomNav, navController)

        val topLevelDestinations = setOf(
            R.id.dashboardFragment,
            R.id.transactionsFragment,
            R.id.goalsFragment,
            R.id.moreFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevel = destination.id in topLevelDestinations
            setBottomBarVisibility(isTopLevel)
        }
    }

    private fun setBottomBarVisibility(visible: Boolean) {
        if (visible) {
            binding.bottomNav.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(250)
                .withStartAction { binding.bottomNav.isVisible = true }
                .start()

            binding.fabAdd.animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .withStartAction { binding.fabAdd.isVisible = true }
                .start()
        } else {
            val hideTranslation = binding.bottomNav.height.toFloat().coerceAtLeast(300f)
            binding.bottomNav.animate()
                .translationY(hideTranslation)
                .alpha(0f)
                .setDuration(200)
                .withEndAction { binding.bottomNav.isVisible = false }
                .start()

            binding.fabAdd.animate()
                .translationY(hideTranslation)
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(200)
                .withEndAction { binding.fabAdd.isVisible = false }
                .start()
        }
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            AddTransactionBottomSheetFragment.newInstance()
                .show(supportFragmentManager, AddTransactionBottomSheetFragment.TAG)
        }
    }
}