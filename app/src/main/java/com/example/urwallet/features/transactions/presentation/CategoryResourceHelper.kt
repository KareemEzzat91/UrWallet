package com.example.urwallet.features.transactions.presentation

import android.graphics.Color
import androidx.annotation.DrawableRes
import com.example.urwallet.R

object CategoryResourceHelper {

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
            else -> R.drawable.ic_other
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
