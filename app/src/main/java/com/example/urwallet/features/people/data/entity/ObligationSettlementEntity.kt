package com.example.urwallet.features.people.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.urwallet.features.people.domain.model.ObligationSettlement

@Entity(
    tableName = "obligation_settlements",
    foreignKeys = [
        ForeignKey(
            entity = FinancialObligationEntity::class,
            parentColumns = ["id"],
            childColumns = ["obligationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("obligationId")
    ]
)
data class ObligationSettlementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val obligationId: Long,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
    val relatedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ObligationSettlement = ObligationSettlement(
        id = id,
        obligationId = obligationId,
        amount = amount,
        date = date,
        note = note,
        relatedTransactionId = relatedTransactionId,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(settlement: ObligationSettlement): ObligationSettlementEntity =
            ObligationSettlementEntity(
                id = settlement.id,
                obligationId = settlement.obligationId,
                amount = settlement.amount,
                date = settlement.date,
                note = settlement.note?.trim()?.ifBlank { null },
                relatedTransactionId = settlement.relatedTransactionId,
                createdAt = settlement.createdAt
            )
    }
}
