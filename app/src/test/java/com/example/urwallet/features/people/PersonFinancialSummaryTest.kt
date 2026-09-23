package com.example.urwallet.features.people

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.people.domain.model.FinancialObligation
import com.example.urwallet.features.people.domain.model.ObligationDirection
import com.example.urwallet.features.people.domain.model.ObligationStatus
import com.example.urwallet.features.people.domain.model.Person
import com.example.urwallet.features.people.domain.model.PersonFinancialSummary
import com.example.urwallet.features.transactions.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonFinancialSummaryTest {

    private val testPerson = Person(
        id = 1L,
        name = "محمد أحمد",
        phoneNumber = "01012345678",
        notes = "صديق"
    )

    @Test
    fun `summary correctly computes net transaction flow`() {
        val summary = PersonFinancialSummary(
            person = testPerson,
            totalSent = 500.0,
            totalReceived = 200.0,
            totalOwedToMe = 0.0,
            totalIOwe = 0.0,
            openObligationsCount = 0
        )

        // Net transaction flow = received (200) - sent (500) = -300
        assertEquals(-300.0, summary.netTransactionFlow, 0.001)
    }

    @Test
    fun `summary correctly computes net obligation balance`() {
        val summary = PersonFinancialSummary(
            person = testPerson,
            totalSent = 0.0,
            totalReceived = 0.0,
            totalOwedToMe = 1000.0,
            totalIOwe = 400.0,
            openObligationsCount = 2
        )

        // Net obligation balance = owed to me (1000) - I owe (400) = +600
        assertEquals(600.0, summary.netObligationBalance, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `financial obligation enforces remaining amount does not exceed amount`() {
        FinancialObligation(
            id = 1L,
            personId = 1L,
            reason = "سلفة",
            amount = 500.0,
            settledAmount = 0.0,
            remainingAmount = 600.0, // Invalid! remaining > original
            direction = ObligationDirection.OWED_TO_ME,
            status = ObligationStatus.OPEN
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `financial obligation enforces non-negative remaining amount`() {
        FinancialObligation(
            id = 1L,
            personId = 1L,
            reason = "سلفة",
            amount = 500.0,
            settledAmount = 600.0,
            remainingAmount = -100.0, // Invalid! negative remaining
            direction = ObligationDirection.OWED_TO_ME,
            status = ObligationStatus.SETTLED
        )
    }
}
