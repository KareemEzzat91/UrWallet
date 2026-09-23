package com.example.urwallet.features.people.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.urwallet.features.people.data.entity.FinancialObligationEntity
import com.example.urwallet.features.people.data.entity.ObligationSettlementEntity
import com.example.urwallet.features.people.domain.model.ObligationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialObligationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObligation(obligation: FinancialObligationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObligations(obligations: List<FinancialObligationEntity>): List<Long>

    @Update
    suspend fun updateObligation(obligation: FinancialObligationEntity): Int

    @Delete
    suspend fun deleteObligation(obligation: FinancialObligationEntity): Int

    @Query("DELETE FROM financial_obligations WHERE id = :id")
    suspend fun deleteObligationById(id: Long): Int

    @Query("SELECT * FROM financial_obligations WHERE id = :id LIMIT 1")
    fun getObligationById(id: Long): Flow<FinancialObligationEntity?>

    @Query("SELECT * FROM financial_obligations WHERE id = :id LIMIT 1")
    suspend fun getObligationByIdSync(id: Long): FinancialObligationEntity?

    @Query("SELECT * FROM financial_obligations WHERE personId = :personId ORDER BY createdAt DESC")
    fun getObligationsByPerson(personId: Long): Flow<List<FinancialObligationEntity>>

    @Query("SELECT * FROM financial_obligations WHERE personId = :personId AND status != 'SETTLED' ORDER BY createdAt DESC")
    suspend fun getActiveObligationsByPersonSync(personId: Long): List<FinancialObligationEntity>

    @Query("SELECT * FROM financial_obligations WHERE status != 'SETTLED' ORDER BY dueDate ASC, createdAt DESC")
    fun getActiveObligations(): Flow<List<FinancialObligationEntity>>

    @Query("SELECT * FROM financial_obligations ORDER BY createdAt DESC")
    fun getAllObligations(): Flow<List<FinancialObligationEntity>>

    @Query("SELECT * FROM financial_obligations ORDER BY createdAt DESC")
    suspend fun getAllObligationsSync(): List<FinancialObligationEntity>

    @Query("DELETE FROM financial_obligations WHERE personId = :personId")
    suspend fun deleteObligationsByPerson(personId: Long): Int

    @Query("DELETE FROM financial_obligations")
    suspend fun deleteAllObligations(): Int

    // --- Settlements ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: ObligationSettlementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(settlements: List<ObligationSettlementEntity>): List<Long>

    @Query("SELECT * FROM obligation_settlements WHERE obligationId = :obligationId ORDER BY date DESC")
    fun getSettlementsForObligation(obligationId: Long): Flow<List<ObligationSettlementEntity>>

    @Query("SELECT * FROM obligation_settlements WHERE obligationId = :obligationId ORDER BY date DESC")
    suspend fun getSettlementsForObligationSync(obligationId: Long): List<ObligationSettlementEntity>

    @Query("SELECT * FROM obligation_settlements ORDER BY date DESC")
    suspend fun getAllSettlementsSync(): List<ObligationSettlementEntity>

    @Query("DELETE FROM obligation_settlements")
    suspend fun deleteAllSettlements(): Int

    /**
     * ATOMIC SETTLEMENT:
     * Records settlement, updates settledAmount, updates remainingAmount, and updates status
     * inside a single Room database transaction.
     * Enforces the invariant: 0 <= remainingAmount <= original amount.
     */
    @Transaction
    suspend fun recordSettlementAtomic(
        settlement: ObligationSettlementEntity
    ): FinancialObligationEntity {
        val obligation = getObligationByIdSync(settlement.obligationId)
            ?: throw IllegalArgumentException("Obligation not found: ${settlement.obligationId}")

        require(settlement.amount > 0.0) { "Settlement amount must be positive" }
        require(settlement.amount <= obligation.remainingAmount + 0.001) {
            "Settlement amount (${settlement.amount}) cannot exceed remaining amount (${obligation.remainingAmount})"
        }

        val newSettled = (obligation.settledAmount + settlement.amount).coerceIn(0.0, obligation.amount)
        val newRemaining = (obligation.amount - newSettled).coerceIn(0.0, obligation.amount)
        val newStatus = when {
            newRemaining <= 0.0001 -> ObligationStatus.SETTLED
            newSettled > 0.0 -> ObligationStatus.PARTIALLY_SETTLED
            else -> ObligationStatus.OPEN
        }

        insertSettlement(settlement)

        val updated = obligation.copy(
            settledAmount = newSettled,
            remainingAmount = newRemaining,
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        updateObligation(updated)
        return updated
    }
}
