package com.example.urwallet.features.analytics.domain.classifier

enum class SpendingNeedType {
    NEED,
    WANT
}

/**
 * Centralized classifier for 50/30/20 rule.
 * Categorizes expenses strictly into NEED or WANT.
 * (Savings is handled as explicit goal contributions / surplus, never an expense category).
 */
object SpendingCategoryClassifier {

    fun classify(icon: String, name: String = ""): SpendingNeedType {
        val lowerIcon = icon.lowercase().trim()
        val lowerName = name.lowercase().trim()

        return when {
            // Needs: bills, transport, health/pharmacy, education
            lowerIcon in setOf("ic_bills", "ic_transport", "ic_health", "ic_education") -> SpendingNeedType.NEED
            lowerName.contains("فاتورة") || lowerName.contains("فواتير") ||
                    lowerName.contains("مواصلات") || lowerName.contains("صحة") ||
                    lowerName.contains("علاج") || lowerName.contains("دواء") ||
                    lowerName.contains("تعليم") || lowerName.contains("دراسة") ||
                    lowerName.contains("إيجار") || lowerName.contains("ايجار") -> SpendingNeedType.NEED

            // Food: Groceries/Home Food = NEED, Restaurants/Cafes = WANT
            lowerIcon == "ic_food" -> {
                if (lowerName.contains("مطعم") || lowerName.contains("كافيه") ||
                    lowerName.contains("قهوة") || lowerName.contains("خروج") ||
                    lowerName.contains("سناك") || lowerName.contains("حلويات")
                ) {
                    SpendingNeedType.WANT
                } else {
                    SpendingNeedType.NEED
                }
            }

            // Wants: shopping, entertainment, etc.
            lowerIcon in setOf("ic_shopping", "ic_entertainment") -> SpendingNeedType.WANT
            lowerName.contains("تسوق") || lowerName.contains("ترفيه") ||
                    lowerName.contains("ألعاب") || lowerName.contains("العاب") ||
                    lowerName.contains("سفر") || lowerName.contains("هدايا") -> SpendingNeedType.WANT

            else -> SpendingNeedType.WANT
        }
    }
}
