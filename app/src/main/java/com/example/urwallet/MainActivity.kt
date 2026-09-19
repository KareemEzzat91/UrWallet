package com.example.urwallet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.urwallet.databinding.ActivityMainBinding
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import com.example.urwallet.features.transactions.presentation.AddTransactionBottomSheetFragment
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.urwallet.features.security.domain.session.AppLockManager
import com.example.urwallet.features.security.presentation.lock.AppLockActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var getSecuritySettingsUseCase: com.example.urwallet.features.security.domain.usecase.GetSecuritySettingsUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL

        if (appLockManager.shouldLockOnForeground()) {
            binding.root.isVisible = false
            AppLockActivity.start(this)
        }

        setupEdgeToEdge()
        setupNavigation()
        setupFab()
        handleNotificationIntent(intent)
        observeSecurityFlags()
    }

    override fun onResume() {
        super.onResume()
        if (appLockManager.shouldLockOnForeground()) {
            binding.root.isVisible = false
            AppLockActivity.start(this)
        } else {
            binding.root.isVisible = true
            lifecycleScope.launch {
                if (appLockManager.shouldLockSuspend()) {
                    binding.root.isVisible = false
                    AppLockActivity.start(this@MainActivity)
                } else {
                    binding.root.isVisible = true
                }
            }
        }
    }

    private fun observeSecurityFlags() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                getSecuritySettingsUseCase().collect { settings ->
                    if (settings.isAppLockEnabled) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val target = intent?.getStringExtra(NotificationHelper.EXTRA_NAV_TARGET) ?: return
        intent.removeExtra(NotificationHelper.EXTRA_NAV_TARGET)

        when (target) {
            NotificationHelper.NAV_TARGET_ADD_TRANSACTION -> {
                if (supportFragmentManager.findFragmentByTag(AddTransactionBottomSheetFragment.TAG) == null) {
                    AddTransactionBottomSheetFragment.newInstance()
                        .show(supportFragmentManager, AddTransactionBottomSheetFragment.TAG)
                }
            }
            NotificationHelper.NAV_TARGET_BUDGETS -> {
                navController.navigate(R.id.budgetsFragment)
            }
            NotificationHelper.NAV_TARGET_GOALS -> {
                binding.bottomNav.selectedItemId = R.id.goalsFragment
            }
            NotificationHelper.NAV_TARGET_SETTINGS -> {
                navController.navigate(R.id.notificationSettingsFragment)
            }
        }
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

        // Connect BottomNavigationView to NavController with Center Placeholder support
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_placeholder -> {
                    AddTransactionBottomSheetFragment.newInstance()
                        .show(supportFragmentManager, AddTransactionBottomSheetFragment.TAG)
                    false
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(item, navController)
                }
            }
        }

        val topLevelDestinations = setOf(
            R.id.dashboardFragment,
            R.id.goalsFragment,
            R.id.transactionsFragment,
            R.id.moreFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevel = destination.id in topLevelDestinations
            setBottomBarVisibility(isTopLevel)
            val menuItem = binding.bottomNav.menu.findItem(destination.id)
            if (menuItem != null && !menuItem.isChecked) {
                menuItem.isChecked = true
            }
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