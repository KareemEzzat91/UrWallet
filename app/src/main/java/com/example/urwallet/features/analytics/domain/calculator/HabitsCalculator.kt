package com.example.urwallet.features.analytics.domain.calculator

import com.example.urwallet.core.common.DateUtils
import com.example.urwallet.features.analytics.domain.classifier.SpendingCategoryClassifier
import com.example.urwallet.features.analytics.domain.classifier.SpendingNeedType
import com.example.urwallet.features.analytics.domain.model.FinancialPersonality
import com.example.urwallet.features.analytics.domain.model.PersonalityType
import com.example.urwallet.features.analytics.domain.model.Rule50_30_20Breakdown
import com.example.urwallet.features.transactions.domain.model.Category
import com.example.urwallet.features.transactions.domain.model.Transaction
import java.util.Calendar

/**
 * Pure calculation engine for Financial Habits:
 * - Peak spending day detection
 * - Daily average expenditure
 * - 50/30/20 rule breakdown
 * - Financial personality diagnosis
 *
 * No Room access. No Android UI dependencies.
 */
object HabitsCalculator {

    private val WEEKDAY_NAMES_AR = mapOf(
        Calendar.SATURDAY to "السبت",
        Calendar.SUNDAY to "الأحد",
        Calendar.MONDAY to "الاثنين",
        Calendar.TUESDAY to "الثلاثاء",
        Calendar.WEDNESDAY to "الأربعاء",
        Calendar.THURSDAY to "الخميس",
        Calendar.FRIDAY to "الجمعة"
    )

    fun calculatePeakSpendingDay(expenseTransactions: List<Transaction>): Pair<String?, Double> {
        if (expenseTransactions.isEmpty()) {
            return Pair(null, 0.0)
        }

        val weekdayTotals = mutableMapOf<Int, Double>()
        for (tx in expenseTransactions) {
            val dayOfWeek = DateUtils.getDayOfWeek(tx.date)
            weekdayTotals[dayOfWeek] = (weekdayTotals[dayOfWeek] ?: 0.0) + tx.amount
        }

        val maxEntry = weekdayTotals.maxByOrNull { it.value }
        return if (maxEntry != null && maxEntry.value > 0.0) {
            val dayName = WEEKDAY_NAMES_AR[maxEntry.key] ?: "غير محدد"
            Pair(dayName, maxEntry.value)
        } else {
            Pair(null, 0.0)
        }
    }

    fun calculateDailyAverage(
        totalExpenses: Double,
        month: Int,
        year: Int,
        currentMonth: Int = DateUtils.getCurrentMonth(),
        currentYear: Int = DateUtils.getCurrentYear(),
        currentDayOfMonth: Int = DateUtils.getCurrentDayOfMonth()
    ): Double {
        if (totalExpenses <= 0.0) return 0.0

        val daysCount = when {
            // Current month: use elapsed days so average is not artificially diluted by future days
            year == currentYear && month == currentMonth -> currentDayOfMonth.coerceAtLeast(1)
            // Historical month: use total days in that month
            else -> DateUtils.getDaysInMonth(month, year).coerceAtLeast(1)
        }

        return totalExpenses / daysCount
    }

    fun calculateRule50_30_20(
        expenseTransactions: List<Transaction>,
        categoriesMap: Map<Long, Category>,
        goalContributions: Double,
        totalIncome: Double
    ): Rule50_30_20Breakdown {
        var needsAmount = 0.0
        var wantsAmount = 0.0

        for (tx in expenseTransactions) {
            val cat = categoriesMap[tx.categoryId]
            val icon = cat?.icon ?: "ic_other"
            val name = cat?.name ?: tx.title

            when (SpendingCategoryClassifier.classify(icon, name)) {
                SpendingNeedType.NEED -> needsAmount += tx.amount
                SpendingNeedType.WANT -> wantsAmount += tx.amount
            }
        }

        val savingsAmount = goalContributions.coerceAtLeast(0.0)

        // Denominator: use total income if available, otherwise total outflow + savings
        val totalBase = if (totalIncome > 0.0) {
            totalIncome
        } else {
            needsAmount + wantsAmount + savingsAmount
        }

        val needsPercentage = if (totalBase > 0.0) (needsAmount / totalBase) * 100.0 else 0.0
        val wantsPercentage = if (totalBase > 0.0) (wantsAmount / totalBase) * 100.0 else 0.0
        val savingsPercentage = if (totalBase > 0.0) (savingsAmount / totalBase) * 100.0 else 0.0

        return Rule50_30_20Breakdown(
            needsAmount = needsAmount,
            wantsAmount = wantsAmount,
            savingsAmount = savingsAmount,
            needsPercentage = needsPercentage,
            wantsPercentage = wantsPercentage,
            savingsPercentage = savingsPercentage
        )
    }

    fun determinePersonality(
        savingsRate: Double,
        hasGlobalBudget: Boolean,
        isBudgetExceeded: Boolean,
        wantsPercentage: Double,
        hasActiveGoals: Boolean,
        goalContributions: Double
    ): FinancialPersonality {
        return when {
            // 1. Cautious Saver: High saving rate or active goal progress with low wants
            (savingsRate >= 25.0 || (hasActiveGoals && goalContributions > 0.0)) && wantsPercentage <= 20.0 -> {
                FinancialPersonality(
                    type = PersonalityType.CAUTIOUS_SAVER,
                    title = "المدخر الحذر",
                    emoji = "🛡️",
                    description = "تحرص بشدة على أمانك المالي وتضع الادخار في قمة أولوياتك، وتتجنب الإنفاق الاستهلاكي غير الضروري.",
                    coachRecommendation = "أمانك المالي ممتاز! لا بأس بتخصيص مكافأة شخصية لنفسك دون أي قلق."
                )
            }

            // 2. Smart Planner: Budget active, disciplined (not exceeded), with good savings
            hasGlobalBudget && !isBudgetExceeded && (savingsRate >= 15.0 || (hasActiveGoals && goalContributions > 0.0)) -> {
                FinancialPersonality(
                    type = PersonalityType.SMART_PLANNER,
                    title = "المخطط الذكي",
                    emoji = "🧠",
                    description = "تحدد ميزانيات واضحة وتلتزم بها بانضباط، مع توجيه الفائض بانتظام نحو أهدافك المالية.",
                    coachRecommendation = "واصل هذا التوازن الاستثنائي وفكّر في بدء تحديات توفير جديدة لتعزيز نموك المالي."
                )
            }

            // 3. Spontaneous Spender: High wants, exceeded budget, or negative cash flow
            isBudgetExceeded || wantsPercentage >= 40.0 || savingsRate < 0.0 -> {
                FinancialPersonality(
                    type = PersonalityType.SPONTANEOUS_SPENDER,
                    title = "المستكشف",
                    emoji = "⚡",
                    description = "تفضل الاستمتاع بالحياة والإنفاق اللحظي، ولكن رغباتك قد تضغط على ميزانيتك ومدخراتك أحياناً.",
                    coachRecommendation = "جرب قاعدة الـ 24 ساعة قبل أي شراء غير ضروري وضع حداً أسبوعياً للمصروفات الترفيهية."
                )
            }

            // 4. Balanced: Default steady state
            else -> {
                FinancialPersonality(
                    type = PersonalityType.BALANCED,
                    title = "المتوازن",
                    emoji = "⚖️",
                    description = "تحافظ على نمط إنفاق متزن يلبي متطلباتك اليومية دون إفراط أو تقتير.",
                    coachRecommendation = "أنت في وضع مستقر! الخطوة التالية هي تحديد هدف ادخاري واضح كل شهر للوصول لمرحلة المخطط الذكي."
                )
            }
        }
    }
}
