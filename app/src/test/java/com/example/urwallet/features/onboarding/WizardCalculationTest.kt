package com.example.urwallet.features.onboarding

import com.example.urwallet.core.common.GoalPaceMode
import org.junit.Assert.assertEquals
import org.junit.Test

class WizardCalculationTest {

    private fun calculatePlan(amount: Double, paceMode: GoalPaceMode): Pair<Int, Double> {
        val months = when (paceMode) {
            GoalPaceMode.RELAXED -> 24
            GoalPaceMode.BALANCED -> 12
            GoalPaceMode.AGGRESSIVE -> 6
        }
        val monthly = if (months > 0) amount / months else amount
        return Pair(months, monthly)
    }

    @Test
    fun testRelaxedPaceDistributesAmountOver24Months() {
        val (months, monthly) = calculatePlan(24000.0, GoalPaceMode.RELAXED)
        assertEquals(24, months)
        assertEquals(1000.0, monthly, 0.001)
    }

    @Test
    fun testBalancedPaceDistributesAmountOver12Months() {
        val (months, monthly) = calculatePlan(12000.0, GoalPaceMode.BALANCED)
        assertEquals(12, months)
        assertEquals(1000.0, monthly, 0.001)
    }

    @Test
    fun testAggressivePaceDistributesAmountOver6Months() {
        val (months, monthly) = calculatePlan(6000.0, GoalPaceMode.AGGRESSIVE)
        assertEquals(6, months)
        assertEquals(1000.0, monthly, 0.001)
    }
}
