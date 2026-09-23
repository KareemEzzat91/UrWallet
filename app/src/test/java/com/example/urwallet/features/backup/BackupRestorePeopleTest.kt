package com.example.urwallet.features.backup

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.backup.data.dto.BackupDataDto
import com.example.urwallet.features.backup.data.dto.BackupPayloadDto
import com.example.urwallet.features.backup.data.dto.CategoryBackupDto
import com.example.urwallet.features.backup.data.dto.FinancialObligationBackupDto
import com.example.urwallet.features.backup.data.dto.ObligationSettlementBackupDto
import com.example.urwallet.features.backup.data.dto.PersonBackupDto
import com.example.urwallet.features.backup.data.dto.TransactionBackupDto
import com.example.urwallet.features.backup.data.parser.BackupJsonParser
import com.example.urwallet.features.backup.data.parser.BackupParseException
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRestorePeopleTest {

    private lateinit var parser: BackupJsonParser

    @Before
    fun setUp() {
        parser = BackupJsonParser()
    }

    @Test
    fun serializeAndParse_withPeopleAndObligations_roundTripMatches() {
        val cat = CategoryBackupDto(
            id = 1L,
            name = "أخرى",
            type = CategoryType.BOTH,
            icon = "ic_other",
            color = "#78909C",
            isDefault = true
        )

        val person = PersonBackupDto(
            id = 10L,
            name = "كريم عز",
            phoneNumber = "01011112222",
            notes = "أخ - ملاحظات",
            createdAt = 1700000000000L
        )

        val tx = TransactionBackupDto(
            id = 100L,
            amount = 150.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "غداء",
            date = 1700001000000L,
            personId = 10L,
            createdAt = 1700001000000L,
            updatedAt = 1700001000000L
        )

        val obligation = FinancialObligationBackupDto(
            id = 200L,
            personId = 10L,
            amount = 1000.0,
            direction = ObligationDirection.OWED_TO_ME,
            status = ObligationStatus.PARTIALLY_SETTLED,
            settledAmount = 300.0,
            remainingAmount = 700.0,
            reason = "سلفة سريعة",
            dueDate = 1705000000000L,
            relatedTransactionId = null,
            createdAt = 1700000000000L,
            updatedAt = 1700002000000L
        )

        val settlement = ObligationSettlementBackupDto(
            id = 300L,
            obligationId = 200L,
            amount = 300.0,
            date = 1700002000000L,
            note = "دفعة أولى",
            relatedTransactionId = 100L,
            createdAt = 1700002000000L
        )

        val payload = BackupPayloadDto(
            version = BackupPayloadDto.CURRENT_BACKUP_VERSION,
            exportedAt = 1700003000000L,
            data = BackupDataDto(
                categories = listOf(cat),
                transactions = listOf(tx),
                people = listOf(person),
                obligations = listOf(obligation),
                obligationSettlements = listOf(settlement)
            )
        )

        val json = parser.serialize(payload)
        val parsed = parser.parseAndValidate(json)

        assertEquals(1, parsed.data.people.size)
        val parsedPerson = parsed.data.people.first()
        assertEquals(10L, parsedPerson.id)
        assertEquals("كريم عز", parsedPerson.name)
        assertEquals("01011112222", parsedPerson.phoneNumber)

        assertEquals(1, parsed.data.obligations.size)
        val parsedOb = parsed.data.obligations.first()
        assertEquals(200L, parsedOb.id)
        assertEquals(10L, parsedOb.personId)
        assertEquals(1000.0, parsedOb.amount, 0.001)
        assertEquals(300.0, parsedOb.settledAmount, 0.001)
        assertEquals(700.0, parsedOb.remainingAmount, 0.001)
        assertEquals(ObligationDirection.OWED_TO_ME, parsedOb.direction)
        assertEquals(ObligationStatus.PARTIALLY_SETTLED, parsedOb.status)

        assertEquals(1, parsed.data.obligationSettlements.size)
        val parsedSettlement = parsed.data.obligationSettlements.first()
        assertEquals(300L, parsedSettlement.id)
        assertEquals(200L, parsedSettlement.obligationId)
        assertEquals(300.0, parsedSettlement.amount, 0.001)
        assertEquals(100L, parsedSettlement.relatedTransactionId)

        assertEquals(10L, parsed.data.transactions.first().personId)
    }

    @Test
    fun parse_obligationWithMissingPerson_throwsInternalReferenceError() {
        val payload = BackupPayloadDto(
            version = BackupPayloadDto.CURRENT_BACKUP_VERSION,
            exportedAt = 1700000000000L,
            data = BackupDataDto(
                categories = listOf(
                    CategoryBackupDto(1L, "أخرى", CategoryType.BOTH, "ic_other", "#78909C")
                ),
                people = emptyList(), // No people
                obligations = listOf(
                    FinancialObligationBackupDto(
                        id = 1L,
                        personId = 999L, // Does not exist
                        amount = 100.0,
                        remainingAmount = 100.0,
                        direction = ObligationDirection.I_OWE,
                        status = ObligationStatus.OPEN,
                        reason = "دين",
                        createdAt = 1700000000000L,
                        updatedAt = 1700000000000L
                    )
                )
            )
        )

        val json = parser.serialize(payload)
        val exception = assertThrows(BackupParseException.InternalReferenceError::class.java) {
            parser.parseAndValidate(json)
        }
        assertTrue(exception.message!!.contains("شخص غير موجود"))
    }

    @Test
    fun parse_settlementWithMissingObligation_throwsInternalReferenceError() {
        val payload = BackupPayloadDto(
            version = BackupPayloadDto.CURRENT_BACKUP_VERSION,
            exportedAt = 1700000000000L,
            data = BackupDataDto(
                categories = listOf(
                    CategoryBackupDto(1L, "أخرى", CategoryType.BOTH, "ic_other", "#78909C")
                ),
                people = listOf(
                    PersonBackupDto(1L, "أحمد", createdAt = 1700000000000L)
                ),
                obligations = emptyList(), // No obligations
                obligationSettlements = listOf(
                    ObligationSettlementBackupDto(
                        id = 1L,
                        obligationId = 888L, // Does not exist
                        amount = 50.0,
                        date = 1700000000000L,
                        createdAt = 1700000000000L
                    )
                )
            )
        )

        val json = parser.serialize(payload)
        val exception = assertThrows(BackupParseException.InternalReferenceError::class.java) {
            parser.parseAndValidate(json)
        }
        assertTrue(exception.message!!.contains("التزام مالي غير موجود"))
    }
}
