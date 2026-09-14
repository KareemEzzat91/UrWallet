package com.example.urwallet.features.notifications

import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.features.goals.domain.model.Goal
import com.example.urwallet.features.goals.domain.model.GoalContribution
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.notifications.data.helper.NotificationHelper
import com.example.urwallet.features.notifications.domain.model.NotificationSettings
import com.example.urwallet.features.notifications.domain.repository.NotificationRepository
import com.example.urwallet.features.notifications.domain.usecase.CheckGoalMilestoneUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckGoalMilestoneUseCaseTest {

    private lateinit var fakeGoalRepo: FakeGoalRepository
    private lateinit var fakeNotificationRepo: FakeNotificationRepository
    private lateinit var fakeNotificationHelper: FakeNotificationHelper
    private lateinit var checkGoalMilestoneUseCase: CheckGoalMilestoneUseCase

    @Before
    fun setup() {
        fakeGoalRepo = FakeGoalRepository()
        fakeNotificationRepo = FakeNotificationRepository()
        fakeNotificationHelper = FakeNotificationHelper()

        checkGoalMilestoneUseCase = CheckGoalMilestoneUseCase(
            goalRepository = fakeGoalRepo,
            notificationRepository = fakeNotificationRepo,
            notificationHelper = fakeNotificationHelper
        )
    }

    @Test
    fun goalMilestone_thresholdCrossing_45_to_50_sendsMilestoneNotification() = runTest {
        val goalId = 1L
        // Target = 1000, currentSaved = 500 (50%), contribution = 50 -> previous was 450 (45%)
        fakeGoalRepo.goals[goalId] = Goal(
            id = goalId,
            name = "رحلة الصيف",
            targetAmount = 1000.0,
            savedAmount = 500.0,
            deadline = 0L,
            paceMode = GoalPaceMode.BALANCED,
            icon = "ic_car",
            monthlyTarget = 100.0
        )

        checkGoalMilestoneUseCase(goalId = goalId, contributionAmount = 50.0)

        assertEquals(1, fakeNotificationHelper.dispatchedMilestones.size)
        val milestone = fakeNotificationHelper.dispatchedMilestones.first()
        assertEquals("رحلة الصيف", milestone.goalName)
        assertEquals(50, milestone.milestonePercentage)
        assertEquals(false, milestone.isCompleted)
    }

    @Test
    fun goalMilestone_thresholdCrossing_49_to_51_sendsMilestoneNotification() = runTest {
        val goalId = 1L
        // Target = 1000, currentSaved = 510 (51%), contribution = 20 -> previous was 490 (49%)
        fakeGoalRepo.goals[goalId] = Goal(
            id = goalId,
            name = "رحلة الصيف",
            targetAmount = 1000.0,
            savedAmount = 510.0,
            deadline = 0L,
            paceMode = GoalPaceMode.BALANCED,
            icon = "ic_car",
            monthlyTarget = 100.0
        )

        checkGoalMilestoneUseCase(goalId = goalId, contributionAmount = 20.0)

        assertEquals(1, fakeNotificationHelper.dispatchedMilestones.size)
        val milestone = fakeNotificationHelper.dispatchedMilestones.first()
        assertEquals(50, milestone.milestonePercentage)
        assertEquals(false, milestone.isCompleted)
    }

    @Test
    fun goalMilestone_thresholdNotCrossing_50_to_60_doesNotSendDuplicate() = runTest {
        val goalId = 1L
        // Target = 1000, currentSaved = 600 (60%), contribution = 100 -> previous was 500 (50%)
        fakeGoalRepo.goals[goalId] = Goal(
            id = goalId,
            name = "رحلة الصيف",
            targetAmount = 1000.0,
            savedAmount = 600.0,
            deadline = 0L,
            paceMode = GoalPaceMode.BALANCED,
            icon = "ic_car",
            monthlyTarget = 100.0
        )

        checkGoalMilestoneUseCase(goalId = goalId, contributionAmount = 100.0)

        // Previous was already 50%, not < 50%, so not crossing
        assertEquals(0, fakeNotificationHelper.dispatchedMilestones.size)
    }

    @Test
    fun goalCompletion_thresholdCrossing_95_to_100_sendsCompletionNotification() = runTest {
        val goalId = 2L
        // Target = 2000, currentSaved = 2000 (100%), contribution = 100 -> previous was 1900 (95%)
        fakeGoalRepo.goals[goalId] = Goal(
            id = goalId,
            name = "لابتوب جديد",
            targetAmount = 2000.0,
            savedAmount = 2000.0,
            deadline = 0L,
            paceMode = GoalPaceMode.AGGRESSIVE,
            icon = "ic_laptop",
            monthlyTarget = 300.0
        )

        checkGoalMilestoneUseCase(goalId = goalId, contributionAmount = 100.0)

        assertEquals(1, fakeNotificationHelper.dispatchedMilestones.size)
        val milestone = fakeNotificationHelper.dispatchedMilestones.first()
        assertEquals("لابتوب جديد", milestone.goalName)
        assertEquals(100, milestone.milestonePercentage)
        assertEquals(true, milestone.isCompleted)
    }

    @Test
    fun goalCompletion_thresholdNotCrossing_100_to_100_doesNotSendDuplicate() = runTest {
        val goalId = 2L
        // Target = 2000, currentSaved = 2100 (105%), contribution = 100 -> previous was 2000 (100%)
        fakeGoalRepo.goals[goalId] = Goal(
            id = goalId,
            name = "لابتوب جديد",
            targetAmount = 2000.0,
            savedAmount = 2100.0,
            deadline = 0L,
            paceMode = GoalPaceMode.AGGRESSIVE,
            icon = "ic_laptop",
            monthlyTarget = 300.0
        )

        checkGoalMilestoneUseCase(goalId = goalId, contributionAmount = 100.0)

        // Previous was already 100%, so 100% completion was not crossed by this contribution
        assertEquals(0, fakeNotificationHelper.dispatchedMilestones.size)
    }

    @Test
    fun alertsDisabled_doesNotSendMilestoneNotification() = runTest {
        fakeNotificationRepo.setGoalAlertsEnabled(false)

        val goalId = 1L
        fakeGoalRepo.goals[goalId] = Goal(
            id = goalId,
            name = "رحلة الصيف",
            targetAmount = 1000.0,
            savedAmount = 500.0,
            deadline = 0L,
            paceMode = GoalPaceMode.BALANCED,
            icon = "ic_car",
            monthlyTarget = 100.0
        )

        checkGoalMilestoneUseCase(goalId = goalId, contributionAmount = 50.0)

        assertTrue(fakeNotificationHelper.dispatchedMilestones.isEmpty())
    }

    @Test
    fun idempotency_repeatedCallForDeliveredMilestone_doesNotSendAgain() = runTest {
        val goalId = 1L
        fakeGoalRepo.goals[goalId] = Goal(
            id = goalId,
            name = "رحلة الصيف",
            targetAmount = 1000.0,
            savedAmount = 500.0,
            deadline = 0L,
            paceMode = GoalPaceMode.BALANCED,
            icon = "ic_car",
            monthlyTarget = 100.0
        )

        // First call sends notification and records delivery key
        checkGoalMilestoneUseCase(goalId = goalId, contributionAmount = 50.0)
        assertEquals(1, fakeNotificationHelper.dispatchedMilestones.size)

        // Repeated call (process death / repeat)
        checkGoalMilestoneUseCase(goalId = goalId, contributionAmount = 50.0)
        assertEquals(1, fakeNotificationHelper.dispatchedMilestones.size)
    }

    // --- Fake Implementations ---

    data class DispatchedGoalMilestone(
        val goalName: String,
        val milestonePercentage: Int,
        val isCompleted: Boolean,
        val goalId: Long
    )

    private class FakeNotificationHelper : NotificationHelper() {
        val dispatchedMilestones = mutableListOf<DispatchedGoalMilestone>()

        override fun hasNotificationPermission(): Boolean = true

        override fun showGoalMilestoneNotification(
            goalName: String,
            milestonePercentage: Int,
            isCompleted: Boolean,
            goalId: Long
        ) {
            dispatchedMilestones.add(
                DispatchedGoalMilestone(goalName, milestonePercentage, isCompleted, goalId)
            )
        }
    }

    private class FakeNotificationRepository : NotificationRepository {
        private val _settings = MutableStateFlow(NotificationSettings())
        private val deliveredKeys = mutableSetOf<String>()

        override fun getNotificationSettings(): Flow<NotificationSettings> = _settings

        override suspend fun setDailyReminderEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(isDailyReminderEnabled = enabled)
        }

        override suspend fun setReminderTime(hour: Int, minute: Int) {
            _settings.value = _settings.value.copy(reminderHour = hour, reminderMinute = minute)
        }

        override suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(isBudgetAlertsEnabled = enabled)
        }

        override suspend fun setGoalAlertsEnabled(enabled: Boolean) {
            _settings.value = _settings.value.copy(isGoalAlertsEnabled = enabled)
        }

        override suspend fun markAlertDelivered(key: String) {
            deliveredKeys.add(key)
        }

        override suspend fun isAlertDelivered(key: String): Boolean {
            return deliveredKeys.contains(key)
        }

        override fun scheduleDailyReminder(hour: Int, minute: Int) {}
        override fun cancelDailyReminder() {}
    }

    private class FakeGoalRepository : GoalRepository {
        val goals = mutableMapOf<Long, Goal>()

        override fun getAllGoals(): Flow<List<Goal>> = flowOf(goals.values.toList())
        override fun getActiveGoals(): Flow<List<Goal>> = flowOf(goals.values.filter { !it.isCompleted })
        override fun getNearestActiveGoal(): Flow<Goal?> = flowOf(goals.values.firstOrNull())
        override fun getGoalById(id: Long): Flow<Goal?> = flowOf(goals[id])
        override fun getContributionsForGoal(goalId: Long): Flow<List<GoalContribution>> = flowOf(emptyList())
        override fun getMonthlyContributionsSum(startDate: Long, endDate: Long): Flow<Double> = flowOf(0.0)
        override suspend fun insertGoal(goal: Goal): Long = 1L
        override suspend fun updateGoal(goal: Goal) {}
        override suspend fun addContribution(goalId: Long, amount: Double, note: String?): Long = 1L
        override suspend fun deleteGoal(id: Long) {}
    }
}
