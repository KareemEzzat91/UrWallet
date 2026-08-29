package com.example.urwallet.features.more.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.data.entity.CategoryEntity

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("categoryId"),
        Index("nextOccurrence")
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val frequency: Frequency,
    val startDate: Long,
    val endDate: Long? = null,
    val nextOccurrence: Long,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
