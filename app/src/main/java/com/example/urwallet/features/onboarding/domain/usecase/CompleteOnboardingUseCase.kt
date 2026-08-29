package com.example.urwallet.features.onboarding.domain.usecase

import com.example.urwallet.core.datastore.AppPreferences
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val appPreferences: AppPreferences
) {
    suspend operator fun invoke() {
        appPreferences.setOnboardingCompleted(true)
    }
}
