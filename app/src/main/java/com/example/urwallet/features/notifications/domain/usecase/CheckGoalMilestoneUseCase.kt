package com.example.urwallet.features.notifications.domain.usecase

import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import com.example.urwallet.features.notifications.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckGoalMilestoneUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationHelper: NotificationHelper
) {

    suspend operator fun invoke(goalId: Long, contributionAmount: Double) {
        try {
            val settings = notificationRepository.getNotificationSettings().first()
            if (!settings.isGoalAlertsEnabled) return

            val goal = goalRepository.getGoalById(goalId).first() ?: return
            val target = goal.targetAmount
            if (target <= 0.0) return

            val currentSaved = goal.savedAmount
            val prevSaved = (currentSaved - contributionAmount).coerceAtLeast(0.0)

            val prevRatio = prevSaved / target
            val currRatio = currentSaved / target

            if (prevRatio < 1.0 && currRatio >= 1.0) {
                val key = "goal_milestone_100_${goal.id}"
                if (!notificationRepository.isAlertDelivered(key)) {
                    notificationRepository.markAlertDelivered(key)
                    notificationHelper.showGoalMilestoneNotification(
                        goalName = goal.name,
                        milestonePercentage = 100,
                        isCompleted = true,
                        goalId = goal.id
                    )
                }
            } else if (prevRatio < 0.5 && currRatio >= 0.5) {
                val key = "goal_milestone_50_${goal.id}"
                if (!notificationRepository.isAlertDelivered(key)) {
                    notificationRepository.markAlertDelivered(key)
                    notificationHelper.showGoalMilestoneNotification(
                        goalName = goal.name,
                        milestonePercentage = 50,
                        isCompleted = false,
                        goalId = goal.id
                    )
                }
            }
        } catch (_: Exception) {
            // Notifications failure must never break the financial contribution
        }
    }
}
