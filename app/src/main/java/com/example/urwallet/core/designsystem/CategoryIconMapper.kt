package com.example.urwallet.core.designsystem

import android.graphics.Color
import androidx.annotation.DrawableRes
import com.example.urwallet.R

object CategoryIconMapper {

    @DrawableRes
    fun getIconDrawableRes(iconName: String): Int {
        return when (iconName) {
            "ic_food" -> R.drawable.ic_food
            "ic_transport" -> R.drawable.ic_transport
            "ic_shopping" -> R.drawable.ic_shopping
            "ic_bills" -> R.drawable.ic_bills
            "ic_entertainment" -> R.drawable.ic_entertainment
            "ic_health" -> R.drawable.ic_health
            "ic_education" -> R.drawable.ic_education
            "ic_other" -> R.drawable.ic_other
            "ic_salary" -> R.drawable.ic_salary
            "ic_freelance" -> R.drawable.ic_freelance
            "ic_bonus" -> R.drawable.ic_bonus
            "ic_income_other" -> R.drawable.ic_income_other
            // Goal icons (shared & emojis)
            "ic_goal_emergency", "🛡️", "ic_shield" -> R.drawable.ic_shield
            "ic_goal_travel", "✈️" -> R.drawable.ic_goal_travel
            "ic_goal_home", "🏠" -> R.drawable.ic_goal_home
            "ic_goal_retirement", "⏰" -> R.drawable.ic_goal_retirement
            "ic_goal_custom", "⭐" -> R.drawable.ic_goal_custom
            else -> R.drawable.ic_other
        }
    }

    @DrawableRes
    fun getGoalDrawableRes(iconName: String, goalName: String = ""): Int {
        return when (iconName) {
            "ic_goal_emergency", "🛡️", "ic_shield" -> R.drawable.ic_shield
            "ic_goal_travel", "✈️" -> R.drawable.ic_goal_travel
            "ic_goal_home", "🏠" -> R.drawable.ic_goal_home
            "ic_goal_retirement", "⏰" -> R.drawable.ic_goal_retirement
            "ic_goal_custom", "⭐" -> R.drawable.ic_goal_custom
            else -> {
                if (goalName.contains("طوارئ") || goalName.contains("صندوق")) R.drawable.ic_shield
                else if (goalName.contains("سفر") || goalName.contains("سياحة")) R.drawable.ic_goal_travel
                else if (goalName.contains("بيت") || goalName.contains("منزل") || goalName.contains("شقة")) R.drawable.ic_goal_home
                else if (goalName.contains("تقاعد")) R.drawable.ic_goal_retirement
                else R.drawable.ic_nav_goals_filled
            }
        }
    }

    @DrawableRes
    fun getGoalPastelBgRes(iconName: String, goalName: String = ""): Int {
        return when (iconName) {
            "ic_goal_emergency", "🛡️", "ic_shield" -> R.drawable.bg_squircle_pastel_amber
            "ic_goal_travel", "✈️" -> R.drawable.bg_squircle_pastel_blue
            "ic_goal_home", "🏠" -> R.drawable.bg_squircle_pastel_purple
            "ic_goal_retirement", "⏰" -> R.drawable.bg_squircle_pastel_green
            "ic_goal_custom", "⭐" -> R.drawable.bg_squircle_pastel_teal
            else -> {
                if (goalName.contains("طوارئ") || goalName.contains("صندوق")) R.drawable.bg_squircle_pastel_amber
                else if (goalName.contains("سفر") || goalName.contains("سياحة")) R.drawable.bg_squircle_pastel_blue
                else if (goalName.contains("بيت") || goalName.contains("منزل") || goalName.contains("شقة")) R.drawable.bg_squircle_pastel_purple
                else if (goalName.contains("تقاعد")) R.drawable.bg_squircle_pastel_green
                else R.drawable.bg_squircle_pastel_blue
            }
        }
    }

    @androidx.annotation.ColorRes
    fun getGoalIconTintRes(iconName: String, goalName: String = ""): Int {
        return when (iconName) {
            "ic_goal_emergency", "🛡️", "ic_shield" -> R.color.urwallet_warning
            "ic_goal_travel", "✈️" -> R.color.urwallet_accent
            "ic_goal_home", "🏠" -> R.color.urwallet_primary
            "ic_goal_retirement", "⏰" -> R.color.urwallet_income
            "ic_goal_custom", "⭐" -> R.color.urwallet_accent
            else -> {
                if (goalName.contains("طوارئ") || goalName.contains("صندوق")) R.color.urwallet_warning
                else if (goalName.contains("سفر") || goalName.contains("سياحة")) R.color.urwallet_accent
                else if (goalName.contains("بيت") || goalName.contains("منزل") || goalName.contains("شقة")) R.color.urwallet_primary
                else if (goalName.contains("تقاعد")) R.color.urwallet_income
                else R.color.urwallet_primary
            }
        }
    }

    fun parseColorSafely(colorHex: String, defaultColor: Int = Color.parseColor("#78909C")): Int {
        return try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            defaultColor
        }
    }
}
