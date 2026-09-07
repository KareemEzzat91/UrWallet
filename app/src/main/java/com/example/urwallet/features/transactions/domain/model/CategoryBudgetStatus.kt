package com.example.urwallet.features.transactions.domain.model

import com.example.urwallet.features.budgets.domain.calculator.BudgetCalculationResult
import com.example.urwallet.features.budgets.domain.model.Budget

data class CategoryBudgetStatus(
    val budget: Budget?,
    val calculation: BudgetCalculationResult?
)
