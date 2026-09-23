package com.example.urwallet.features.people.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationStatus

@Entity(
    tableName = "financial_obligations",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("personId"),
        Index("status"),
        Index("dueDate")
    ]
)
data class FinancialObligationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personId: Long,
    val amount: Double,
    val direction: ObligationDirection,
    val reason: String? = null,
    val dueDate: Long? = null,
    val status: ObligationStatus = ObligationStatus.OPEN,
    val settledAmount: Double = 0.0,
    val remainingAmount: Double = amount,
    val relatedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): FinancialObligation = FinancialObligation(
        id = id,
        personId = personId,
        amount = amount,
        direction = direction,
        reason = reason,
        dueDate = dueDate,
        status = status,
        settledAmount = settledAmount,
        remainingAmount = remainingAmount,
        relatedTransactionId = relatedTransactionId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(obligation: FinancialObligation): FinancialObligationEntity =
            FinancialObligationEntity(
                id = obligation.id,
                personId = obligation.personId,
                amount = obligation.amount,
                direction = obligation.direction,
                reason = obligation.reason?.trim()?.ifBlank { null },
                dueDate = obligation.dueDate,
                status = obligation.status,
                settledAmount = obligation.settledAmount,
                remainingAmount = obligation.remainingAmount,
                relatedTransactionId = obligation.relatedTransactionId,
                createdAt = obligation.createdAt,
                updatedAt = obligation.updatedAt
            )
    }
}
