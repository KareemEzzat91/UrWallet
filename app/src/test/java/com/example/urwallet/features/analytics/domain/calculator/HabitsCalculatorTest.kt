package com.example.urwallet.features.analytics.domain.calculator

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.analytics.domain.classifier.SpendingCategoryClassifier
import com.example.urwallet.features.analytics.domain.classifier.SpendingNeedType
import com.example.urwallet.features.analytics.domain.model.PersonalityType
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HabitsCalculatorTest {

    @Test
    fun `when no expenses, peak spending day is null`() {
        val (day, amount) = HabitsCalculator.calculatePeakSpendingDay(emptyList())
        assertNull(day)
        assertEquals(0.0, amount, 0.001)
    }

    @Test
    fun `peak spending day identifies day with highest expense sum`() {
        // Create transactions on Saturday vs Monday
        val calSat = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 5) // Saturday
        }
        val calMon = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7) // Monday
        }

        val transactions = listOf(
            Transaction(id = 1L, amount = 1000.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "شراء", date = calSat.timeInMillis),
            Transaction(id = 2L, amount = 500.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "شراء", date = calSat.timeInMillis),
            Transaction(id = 3L, amount = 800.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "شراء", date = calMon.timeInMillis)
        )

        val (day, amount) = HabitsCalculator.calculatePeakSpendingDay(transactions)
        assertEquals("السبت", day)
        assertEquals(1500.0, amount, 0.001)
    }

    @Test
    fun `daily average calculates correctly for current and historical months`() {
        // Zero expenses
        assertEquals(0.0, HabitsCalculator.calculateDailyAverage(0.0, 9, 2026), 0.001)

        // Current month (e.g. day 10 of current month, 3000 spent -> 300/day)
        val currentAvg = HabitsCalculator.calculateDailyAverage(
            totalExpenses = 3000.0,
            month = 9,
            year = 2026,
            currentMonth = 9,
            currentYear = 2026,
            currentDayOfMonth = 10
        )
        assertEquals(300.0, currentAvg, 0.001)

        // Historical month (e.g. April with 30 days, 6000 spent -> 200/day)
        val histAvg = HabitsCalculator.calculateDailyAverage(
            totalExpenses = 6000.0,
            month = 4,
            year = 2026,
            currentMonth = 9,
            currentYear = 2026,
            currentDayOfMonth = 10
        )
        assertEquals(200.0, histAvg, 0.001)
    }

    @Test
    fun `SpendingCategoryClassifier correctly classifies needs vs wants`() {
        assertEquals(SpendingNeedType.NEED, SpendingCategoryClassifier.classify("ic_bills", "كهرباء"))
        assertEquals(SpendingNeedType.NEED, SpendingCategoryClassifier.classify("ic_transport", "مترو"))
        assertEquals(SpendingNeedType.NEED, SpendingCategoryClassifier.classify("ic_health", "صيدلية"))
        assertEquals(SpendingNeedType.NEED, SpendingCategoryClassifier.classify("ic_food", "سوبرماركت"))

        assertEquals(SpendingNeedType.WANT, SpendingCategoryClassifier.classify("ic_food", "كافيه ستاربكس"))
        assertEquals(SpendingNeedType.WANT, SpendingCategoryClassifier.classify("ic_shopping", "ملابس"))
        assertEquals(SpendingNeedType.WANT, SpendingCategoryClassifier.classify("ic_entertainment", "سينما"))
    }

    @Test
    fun `calculateRule50_30_20 breaks down expenses into needs, wants, and savings`() {
        val catBills = Category(id = 1L, name = "فواتير", type = CategoryType.EXPENSE, icon = "ic_bills", color = "#FFA726")
        val catShop = Category(id = 2L, name = "تسوق", type = CategoryType.EXPENSE, icon = "ic_shopping", color = "#AB47BC")
        val categoriesMap = mapOf(1L to catBills, 2L to catShop)

        val transactions = listOf(
            Transaction(id = 1L, amount = 5000.0, type = TransactionType.EXPENSE, categoryId = 1L, title = "كهرباء", date = 1000L),
            Transaction(id = 2L, amount = 3000.0, type = TransactionType.EXPENSE, categoryId = 2L, title = "ملابس", date = 2000L)
        )

        val breakdown = HabitsCalculator.calculateRule50_30_20(
            expenseTransactions = transactions,
            categoriesMap = categoriesMap,
            goalContributions = 2000.0,
            totalIncome = 10000.0
        )

        assertEquals(5000.0, breakdown.needsAmount, 0.001)
        assertEquals(3000.0, breakdown.wantsAmount, 0.001)
        assertEquals(2000.0, breakdown.savingsAmount, 0.001)

        assertEquals(50.0, breakdown.needsPercentage, 0.001)
        assertEquals(30.0, breakdown.wantsPercentage, 0.001)
        assertEquals(20.0, breakdown.savingsPercentage, 0.001)
    }

    @Test
    fun `determinePersonality diagnoses correct money personality`() {
        // 1. Smart Planner
        val smartPlanner = HabitsCalculator.determinePersonality(
            savingsRate = 20.0,
            hasGlobalBudget = true,
            isBudgetExceeded = false,
            wantsPercentage = 25.0,
            hasActiveGoals = true,
            goalContributions = 1000.0
        )
        assertEquals(PersonalityType.SMART_PLANNER, smartPlanner.type)
        assertTrue(smartPlanner.title.contains("المخطط الذكي"))

        // 2. Cautious Saver
        val cautiousSaver = HabitsCalculator.determinePersonality(
            savingsRate = 30.0,
            hasGlobalBudget = false,
            isBudgetExceeded = false,
            wantsPercentage = 15.0,
            hasActiveGoals = true,
            goalContributions = 2000.0
        )
        assertEquals(PersonalityType.CAUTIOUS_SAVER, cautiousSaver.type)
        assertTrue(cautiousSaver.title.contains("المدخر الحذر"))

        // 3. Spontaneous Spender
        val spontaneous = HabitsCalculator.determinePersonality(
            savingsRate = -5.0,
            hasGlobalBudget = true,
            isBudgetExceeded = true,
            wantsPercentage = 45.0,
            hasActiveGoals = false,
            goalContributions = 0.0
        )
        assertEquals(PersonalityType.SPONTANEOUS_SPENDER, spontaneous.type)
        assertTrue(spontaneous.title.contains("المستكشف"))

        // 4. Balanced
        val balanced = HabitsCalculator.determinePersonality(
            savingsRate = 10.0,
            hasGlobalBudget = false,
            isBudgetExceeded = false,
            wantsPercentage = 25.0,
            hasActiveGoals = false,
            goalContributions = 0.0
        )
        assertEquals(PersonalityType.BALANCED, balanced.type)
        assertTrue(balanced.title.contains("المتوازن"))
    }
}
