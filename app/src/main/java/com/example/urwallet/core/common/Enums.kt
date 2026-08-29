package com.example.urwallet.core.common

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class CategoryType {
    INCOME,
    EXPENSE,
    BOTH
}

enum class GoalPaceMode {
    RELAXED,
    BALANCED,
    AGGRESSIVE
}

enum class BudgetStatus {
    HEALTHY,    // < 80% (Green)
    NEAR_LIMIT, // 80% - 99% (Yellow)
    EXCEEDED    // >= 100% (Red)
}

enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class ChallengeType {
    NO_SPENDING,
    SAVE_AMOUNT,
    REDUCE_CATEGORY
}
