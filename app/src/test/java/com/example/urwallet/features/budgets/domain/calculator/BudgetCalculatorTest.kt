package com.example.urwallet.features.budgets.domain.calculator

import com.example.urwallet.core.common.BudgetStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetCalculatorTest {

    @Test
    fun `when spending is below 80 percent, status should be HEALTHY`() {
        val result = BudgetCalculator.evaluate(spent = 700.0, budgetAmount = 1000.0)
        assertEquals(70.0, result.percentage, 0.001)
        assertEquals(300.0, result.remainingAmount, 0.001)
        assertEquals(BudgetStatus.HEALTHY, result.status)
    }

    @Test
    fun `when spending is between 80 and 99 percent, status should be NEAR_LIMIT`() {
        val result = BudgetCalculator.evaluate(spent = 850.0, budgetAmount = 1000.0)
        assertEquals(85.0, result.percentage, 0.001)
        assertEquals(150.0, result.remainingAmount, 0.001)
        assertEquals(BudgetStatus.NEAR_LIMIT, result.status)
    }

    @Test
    fun `when spending is at or above 100 percent, status should be EXCEEDED`() {
        val result = BudgetCalculator.evaluate(spent = 1100.0, budgetAmount = 1000.0)
        assertEquals(110.0, result.percentage, 0.001)
        assertEquals(0.0, result.remainingAmount, 0.001)
        assertEquals(BudgetStatus.EXCEEDED, result.status)
    }

    @Test
    fun `when budget is zero or negative, return safe values`() {
        val result = BudgetCalculator.evaluate(spent = 50.0, budgetAmount = 0.0)
        assertEquals(0.0, result.percentage, 0.001)
        assertEquals(0.0, result.remainingAmount, 0.001)
        assertEquals(BudgetStatus.HEALTHY, result.status)
    }
}
