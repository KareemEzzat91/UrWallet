package com.example.urwallet.core.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.features.transactions.data.entity.CategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class DatabaseCallback(
    private val databaseProvider: () -> UrWalletDatabase
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            populateDefaultCategories(databaseProvider())
        }
    }

    private suspend fun populateDefaultCategories(database: UrWalletDatabase) {
        val categoryDao = database.categoryDao()
        if (categoryDao.getCategoryCount() > 0) return

        val defaultCategories = listOf(
            // Expense Categories
            CategoryEntity(
                name = "أكل ومشروبات",
                type = CategoryType.EXPENSE,
                icon = "ic_food",
                color = "#FF7043",
                isDefault = true
            ),
            CategoryEntity(
                name = "مواصلات",
                type = CategoryType.EXPENSE,
                icon = "ic_transport",
                color = "#42A5F5",
                isDefault = true
            ),
            CategoryEntity(
                name = "تسوق",
                type = CategoryType.EXPENSE,
                icon = "ic_shopping",
                color = "#AB47BC",
                isDefault = true
            ),
            CategoryEntity(
                name = "فواتير",
                type = CategoryType.EXPENSE,
                icon = "ic_bills",
                color = "#FFA726",
                isDefault = true
            ),
            CategoryEntity(
                name = "ترفيه",
                type = CategoryType.EXPENSE,
                icon = "ic_entertainment",
                color = "#26A69A",
                isDefault = true
            ),
            CategoryEntity(
                name = "صحة",
                type = CategoryType.EXPENSE,
                icon = "ic_health",
                color = "#EF5350",
                isDefault = true
            ),
            CategoryEntity(
                name = "تعليم",
                type = CategoryType.EXPENSE,
                icon = "ic_education",
                color = "#5C6BC0",
                isDefault = true
            ),
            CategoryEntity(
                name = "أخرى",
                type = CategoryType.EXPENSE,
                icon = "ic_other",
                color = "#78909C",
                isDefault = true
            ),

            // Income Categories
            CategoryEntity(
                name = "راتب",
                type = CategoryType.INCOME,
                icon = "ic_salary",
                color = "#00732C",
                isDefault = true
            ),
            CategoryEntity(
                name = "عمل حر",
                type = CategoryType.INCOME,
                icon = "ic_freelance",
                color = "#00897B",
                isDefault = true
            ),
            CategoryEntity(
                name = "مكافأة",
                type = CategoryType.INCOME,
                icon = "ic_bonus",
                color = "#8E24AA",
                isDefault = true
            ),
            CategoryEntity(
                name = "أخرى",
                type = CategoryType.INCOME,
                icon = "ic_income_other",
                color = "#43A047",
                isDefault = true
            )
        )

        categoryDao.insertCategories(defaultCategories)
    }
}
