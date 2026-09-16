package com.example.urwallet.features.backup

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.backup.data.dto.BackupDataDto
import com.example.urwallet.features.backup.data.dto.BackupPayloadDto
import com.example.urwallet.features.backup.data.dto.BudgetBackupDto
import com.example.urwallet.features.backup.data.dto.CategoryBackupDto
import com.example.urwallet.features.backup.data.dto.ChallengeBackupDto
import com.example.urwallet.features.backup.data.dto.GoalBackupDto
import com.example.urwallet.features.backup.data.dto.GoalContributionBackupDto
import com.example.urwallet.features.backup.data.dto.RecurringTransactionBackupDto
import com.example.urwallet.features.backup.data.dto.TransactionBackupDto
import com.example.urwallet.features.backup.data.parser.BackupJsonParser
import com.example.urwallet.features.backup.data.parser.BackupParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupJsonParserTest {

    private lateinit var parser: BackupJsonParser

    @Before
    fun setUp() {
        parser = BackupJsonParser()
    }

    @Test
    fun serializeAndParse_roundTrip_matchesAllEntities() {
        val cat = CategoryBackupDto(
            id = 1L,
            name = "أكل ومشروبات",
            type = CategoryType.EXPENSE,
            icon = "ic_food",
            color = "#FF7043",
            isDefault = true,
            isDeleted = false
        )

        val goal = GoalBackupDto(
            id = 10L,
            name = "شراء حاسوب",
            icon = "ic_education",
            targetAmount = 50000.0,
            paceMode = GoalPaceMode.AGGRESSIVE,
            monthlyTarget = 5000.0,
            deadline = 1789999999000L,
            createdAt = 1788000000000L,
            isDeleted = false
        )

        val contribution = GoalContributionBackupDto(
            id = 100L,
            goalId = 10L,
            amount = 2500.0,
            note = "دفعة شهرية",
            date = 1788500000000L
        )

        val budget = BudgetBackupDto(
            id = 20L,
            categoryId = 1L,
            amount = 4000.0,
            month = 9,
            year = 2026,
            alertThreshold = 0.85,
            createdAt = 1788000000000L
        )

        val recurring = RecurringTransactionBackupDto(
            id = 30L,
            title = "إنترنت منزلي",
            amount = 450.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            frequency = Frequency.MONTHLY,
            startDate = 1788000000000L,
            endDate = null,
            nextOccurrence = 1789000000000L,
            isActive = true,
            createdAt = 1788000000000L
        )

        val transaction = TransactionBackupDto(
            id = 40L,
            amount = 120.0,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "غداء عمل",
            note = "مطعم",
            date = 1788600000000L,
            receiptPath = null,
            createdAt = 1788600000000L,
            updatedAt = 1788600000000L
        )

        val challenge = ChallengeBackupDto(
            id = 50L,
            title = "تحدي 7 أيام بدون شراء قهوة",
            description = "توفير",
            type = ChallengeType.REDUCE_CATEGORY,
            targetAmount = 300.0,
            targetDays = 7,
            categoryId = 1L,
            startDate = 1788000000000L,
            endDate = 1788604800000L,
            currentProgress = 100.0,
            streakDays = 3,
            isCompleted = false,
            isActive = true
        )

        val originalPayload = BackupPayloadDto(
            version = 1,
            exportedAt = 1789123456789L,
            appName = "UrWallet",
            appVersion = "1.0",
            data = BackupDataDto(
                categories = listOf(cat),
                goals = listOf(goal),
                goalContributions = listOf(contribution),
                budgets = listOf(budget),
                recurringTransactions = listOf(recurring),
                transactions = listOf(transaction),
                challenges = listOf(challenge)
            )
        )

        val json = parser.serialize(originalPayload)
        assertNotNull(json)
        assertTrue(json.contains("UrWallet"))

        val parsed = parser.parseAndValidate(json)
        assertEquals(1, parsed.version)
        assertEquals(1, parsed.data.categories.size)
        assertEquals("أكل ومشروبات", parsed.data.categories[0].name)
        assertEquals(1, parsed.data.goals.size)
        assertEquals("شراء حاسوب", parsed.data.goals[0].name)
        assertEquals(1, parsed.data.goalContributions.size)
        assertEquals(2500.0, parsed.data.goalContributions[0].amount, 0.001)
        assertEquals(1, parsed.data.budgets.size)
        assertEquals(4000.0, parsed.data.budgets[0].amount, 0.001)
        assertEquals(1, parsed.data.recurringTransactions.size)
        assertEquals("إنترنت منزلي", parsed.data.recurringTransactions[0].title)
        assertEquals(1, parsed.data.transactions.size)
        assertEquals("غداء عمل", parsed.data.transactions[0].title)
        assertEquals(1, parsed.data.challenges.size)
        assertEquals("تحدي 7 أيام بدون شراء قهوة", parsed.data.challenges[0].title)
    }

    @Test
    fun parseAndValidate_unsupportedVersion_throwsUnsupportedVersion() {
        val json = """
            {
                "version": 2,
                "data": {}
            }
        """.trimIndent()

        assertThrows(BackupParseException.UnsupportedVersion::class.java) {
            parser.parseAndValidate(json)
        }
    }

    @Test
    fun parseAndValidate_malformedJson_throwsMalformedJson() {
        val json = "{ not valid json"
        assertThrows(BackupParseException.MalformedJson::class.java) {
            parser.parseAndValidate(json)
        }
    }

    @Test
    fun parseAndValidate_missingData_throwsInvalidData() {
        val json = """{"version": 1}"""
        assertThrows(BackupParseException.InvalidData::class.java) {
            parser.parseAndValidate(json)
        }
    }

    @Test
    fun parseAndValidate_invalidEnum_throwsInvalidData() {
        val json = """
            {
                "version": 1,
                "data": {
                    "categories": [
                        {
                            "id": 1,
                            "name": "طعام",
                            "type": "UNKNOWN_TYPE"
                        }
                    ]
                }
            }
        """.trimIndent()

        assertThrows(BackupParseException.InvalidData::class.java) {
            parser.parseAndValidate(json)
        }
    }

    @Test
    fun parseAndValidate_brokenCategoryReference_throwsInternalReferenceError() {
        val json = """
            {
                "version": 1,
                "data": {
                    "categories": [
                        {
                            "id": 1,
                            "name": "طعام",
                            "type": "EXPENSE"
                        }
                    ],
                    "transactions": [
                        {
                            "id": 10,
                            "amount": 50.0,
                            "type": "EXPENSE",
                            "categoryId": 999,
                            "title": "معاملة يتيمة",
                            "date": 1788000000000
                        }
                    ]
                }
            }
        """.trimIndent()

        assertThrows(BackupParseException.InternalReferenceError::class.java) {
            parser.parseAndValidate(json)
        }
    }

    @Test
    fun parseAndValidate_brokenGoalReference_throwsInternalReferenceError() {
        val json = """
            {
                "version": 1,
                "data": {
                    "goals": [
                        {
                            "id": 1,
                            "name": "هدف 1",
                            "targetAmount": 1000.0,
                            "paceMode": "BALANCED",
                            "deadline": 1789000000000
                        }
                    ],
                    "goalContributions": [
                        {
                            "id": 10,
                            "goalId": 777,
                            "amount": 200.0,
                            "date": 1788000000000
                        }
                    ]
                }
            }
        """.trimIndent()

        assertThrows(BackupParseException.InternalReferenceError::class.java) {
            parser.parseAndValidate(json)
        }
    }
}
