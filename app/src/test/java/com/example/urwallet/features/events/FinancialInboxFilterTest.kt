package com.example.urwallet.features.events

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.Counterparty
import com.example.urwallet.features.events.domain.model.CounterpartyType
import com.example.urwallet.features.events.domain.model.DuplicateMatchStatus
import com.example.urwallet.features.events.domain.model.EventConfidence
import com.example.urwallet.features.events.domain.model.FinancialEvent
import com.example.urwallet.features.events.domain.model.FinancialEventSource
import com.example.urwallet.features.events.domain.model.InboxFilter
import com.example.urwallet.features.events.domain.model.InboxSortOrder
import com.example.urwallet.features.events.domain.model.InboxStatus
import com.example.urwallet.features.events.domain.model.InboxTab
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FinancialInboxFilterTest {

    private lateinit var fakeEventRepo: FakeFinancialEventRepository

    @Before
    fun setUp() {
        fakeEventRepo = FakeFinancialEventRepository()

        val sampleEvents = listOf(
            FinancialEvent(
                id = 1L,
                amount = 150.0,
                type = TransactionType.EXPENSE,
                date = 1000L,
                sourceIdentifier = "sms_1",
                sender = "CIB",
                status = InboxStatus.PENDING,
                matchStatus = DuplicateMatchStatus.NEW_EVENT,
                counterparty = Counterparty("Starbucks", CounterpartyType.MERCHANT)
            ),
            FinancialEvent(
                id = 2L,
                amount = 500.0,
                type = TransactionType.EXPENSE,
                date = 2000L,
                sourceIdentifier = "sms_2",
                sender = "InstaPay",
                status = InboxStatus.PENDING,
                matchStatus = DuplicateMatchStatus.POSSIBLE_MATCH,
                matchedTransactionId = 99L,
                counterparty = Counterparty("أحمد", CounterpartyType.PERSON, "01011112222")
            ),
            FinancialEvent(
                id = 3L,
                amount = 1200.0,
                type = TransactionType.INCOME,
                date = 3000L,
                sourceIdentifier = "sms_3",
                sender = "VodafoneCash",
                status = InboxStatus.CONFIRMED,
                matchStatus = DuplicateMatchStatus.NEW_EVENT,
                counterparty = Counterparty("محمود", CounterpartyType.PERSON)
            ),
            FinancialEvent(
                id = 4L,
                amount = 50.0,
                type = TransactionType.EXPENSE,
                date = 4000L,
                sourceIdentifier = "sms_4",
                sender = "Fawry",
                status = InboxStatus.DISMISSED,
                matchStatus = DuplicateMatchStatus.NEW_EVENT
            )
        )

        sampleEvents.forEach { fakeEventRepo.events.add(it) }
    }

    @Test
    fun `filter tab PENDING returns only pending events`() = runTest {
        val result = fakeEventRepo.getFilteredEvents(InboxFilter(tab = InboxTab.PENDING)).first()
        assertEquals(2, result.size)
        assertTrue(result.all { it.status == InboxStatus.PENDING })
    }

    @Test
    fun `filter tab POSSIBLE_DUPLICATES returns only pending duplicates`() = runTest {
        val result = fakeEventRepo.getFilteredEvents(InboxFilter(tab = InboxTab.POSSIBLE_DUPLICATE)).first()
        assertEquals(1, result.size)
        assertEquals(2L, result.first().id)
        assertEquals(DuplicateMatchStatus.POSSIBLE_MATCH, result.first().matchStatus)
    }

    @Test
    fun `filter tab CONFIRMED returns only confirmed events`() = runTest {
        val result = fakeEventRepo.getFilteredEvents(InboxFilter(tab = InboxTab.CONFIRMED)).first()
        assertEquals(1, result.size)
        assertEquals(3L, result.first().id)
    }

    @Test
    fun `filter tab DISMISSED returns only dismissed events`() = runTest {
        val result = fakeEventRepo.getFilteredEvents(InboxFilter(tab = InboxTab.DISMISSED)).first()
        assertEquals(1, result.size)
        assertEquals(4L, result.first().id)
    }

    @Test
    fun `filter tab ALL returns all events`() = runTest {
        val result = fakeEventRepo.getFilteredEvents(InboxFilter(tab = InboxTab.ALL)).first()
        assertEquals(4, result.size)
    }

    @Test
    fun `search query filters by counterparty name or sender`() = runTest {
        val byMerchant = fakeEventRepo.getFilteredEvents(
            InboxFilter(tab = InboxTab.ALL, query = "Starbucks")
        ).first()
        assertEquals(1, byMerchant.size)
        assertEquals(1L, byMerchant.first().id)

        val bySender = fakeEventRepo.getFilteredEvents(
            InboxFilter(tab = InboxTab.ALL, query = "InstaPay")
        ).first()
        assertEquals(1, bySender.size)
        assertEquals(2L, bySender.first().id)
    }

    @Test
    fun `sort order works for date and amount`() = runTest {
        val byAmountDesc = fakeEventRepo.getFilteredEvents(
            InboxFilter(tab = InboxTab.ALL, sortOrder = InboxSortOrder.HIGHEST_AMOUNT)
        ).first()
        assertEquals(1200.0, byAmountDesc.first().amount, 0.001)

        val byAmountAsc = fakeEventRepo.getFilteredEvents(
            InboxFilter(tab = InboxTab.ALL, sortOrder = InboxSortOrder.LOWEST_AMOUNT)
        ).first()
        assertEquals(50.0, byAmountAsc.first().amount, 0.001)
    }

    @Test
    fun `bulk actions dismiss and mark pending`() = runTest {
        fakeEventRepo.markDismissedBulk(listOf(1L, 2L))
        val dismissedList = fakeEventRepo.getFilteredEvents(InboxFilter(tab = InboxTab.DISMISSED)).first()
        assertEquals(3, dismissedList.size) // ID 1, 2, and previously 4

        fakeEventRepo.markPendingBulk(listOf(4L))
        val pendingList = fakeEventRepo.getFilteredEvents(InboxFilter(tab = InboxTab.PENDING)).first()
        assertEquals(1, pendingList.size)
        assertEquals(4L, pendingList.first().id)
    }
}
