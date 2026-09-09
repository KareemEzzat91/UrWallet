package com.example.urwallet.features.budgets.domain.calculator

import com.example.urwallet.core.common.BudgetStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetCalculatorTest {

    @Test
    fun `when spending is zero, status is HEALTHY and progress is 0`() {
        val result = BudgetCalculator.evaluate(spent = 0.0, budgetAmount = 1000.0)
        assertEquals(0.0, result.percentage, 0.001)
        assertEquals(0, result.visualProgress)
        assertEquals(1000.0, result.remainingAmount, 0.001)
        assertEquals(BudgetStatus.HEALTHY, result.status)
    }

    @Test
    fun `when spending is 50 percent, status is HEALTHY`() {
        val result = BudgetCalculator.evaluate(spent = 500.0, budgetAmount = 1000.0)
        assertEquals(50.0, result.percentage, 0.001)
        assertEquals(50, result.visualProgress)
        assertEquals(500.0, result.remainingAmount, 0.001)
        assertEquals(BudgetStatus.HEALTHY, result.status)
    }

    @Test
    fun `when spending is below 80 percent, status is HEALTHY`() {
        val result = BudgetCalculator.evaluate(spent = 799.9, budgetAmount = 1000.0)
        assertEquals(79.99, result.percentage, 0.01)
        assertEquals(BudgetStatus.HEALTHY, result.status)
    }

    @Test
    fun `when spending is exactly 80 percent, status is NEAR_LIMIT`() {
        val result = BudgetCalculator.evaluate(spent = 800.0, budgetAmount = 1000.0)
        assertEquals(80.0, result.percentage, 0.001)
        assertEquals(80, result.visualProgress)
        assertEquals(200.0, result.remainingAmount, 0.001)
        assertEquals(BudgetStatus.NEAR_LIMIT, result.status)
    }

    @Test
    fun `when spending is 90 percent, status is NEAR_LIMIT`() {
        val result = BudgetCalculator.evaluate(spent = 900.0, budgetAmount = 1000.0)
        assertEquals(90.0, result.percentage, 0.001)
        assertEquals(100.0, result.remainingAmount, 0.001)
        assertEquals(BudgetStatus.NEAR_LIMIT, result.status)
    }

    @Test
    fun `when spending is exactly 100 percent, status is NEAR_LIMIT`() {
        val result = BudgetCalculator.evaluate(spent = 1000.0, budgetAmount = 1000.0)
        assertEquals(100.0, result.percentage, 0.001)
        assertEquals(100, result.visualProgress)
        assertEquals(0.0, result.remainingAmount, 0.001)
        assertEquals(BudgetStatus.NEAR_LIMIT, result.status)
    }

    @Test
    fun `when spending exceeds 100 percent, status is EXCEEDED and remaining is negative`() {
        val result = BudgetCalculator.evaluate(spent = 3500.0, budgetAmount = 3000.0)
        assertEquals(116.66, result.percentage, 0.01)
        assertEquals(100, result.visualProgress) // Visual progress clamped to 100
        assertEquals(-500.0, result.remainingAmount, 0.001) // Do NOT hide negative remaining
        assertEquals(BudgetStatus.EXCEEDED, result.status)
    }

    @Test
    fun `when budget is zero or negative, return safe values`() {
        val result = BudgetCalculator.evaluate(spent = 50.0, budgetAmount = 0.0)
        assertEquals(0.0, result.percentage, 0.001)
        assertEquals(0, result.visualProgress)
        assertEquals(-50.0, result.remainingAmount, 0.001)
        assertEquals(BudgetStatus.HEALTHY, result.status)
    }
}
