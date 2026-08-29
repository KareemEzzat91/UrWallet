package com.example.urwallet.features.budgets.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.urwallet.features.transactions.data.entity.CategoryEntity

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("categoryId"),
        Index(value = ["categoryId", "month", "year"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long? = null, // null = Global monthly budget
    val amount: Double,
    val month: Int,
    val year: Int,
    val alertThreshold: Double = 0.80,
    val createdAt: Long = System.currentTimeMillis()
)
