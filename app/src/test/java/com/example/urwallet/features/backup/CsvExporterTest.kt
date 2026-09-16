package com.example.urwallet.features.backup

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.backup.data.parser.CsvExporter
import com.example.urwallet.features.transactions.data.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CsvExporterTest {

    private lateinit var csvExporter: CsvExporter

    @Before
    fun setUp() {
        csvExporter = CsvExporter()
    }

    @Test
    fun exportTransactions_startsWithUtf8Bom() {
        val result = csvExporter.exportTransactions(emptyList())
        assertTrue("Output should start with UTF-8 BOM", result.startsWith(CsvExporter.UTF8_BOM))
    }

    @Test
    fun exportTransactions_containsArabicHeaders() {
        val result = csvExporter.exportTransactions(emptyList())
        assertTrue("Output should contain Arabic headers", result.contains(CsvExporter.HEADERS))
    }

    @Test
    fun escapeCsvField_plainText_remainsUnchanged() {
        val input = "فاتورة الكهرباء"
        val result = csvExporter.escapeCsvField(input)
        assertEquals(input, result)
    }

    @Test
    fun escapeCsvField_containsComma_wrappedInQuotes() {
        val input = "سوبرماركت, خضار وفواكه"
        val result = csvExporter.escapeCsvField(input)
        assertEquals("\"$input\"", result)
    }

    @Test
    fun escapeCsvField_containsDoubleQuotes_escapedCorrectly() {
        val input = "مطعم \"البرنس\""
        val expected = "\"مطعم \"\"البرنس\"\"\""
        val result = csvExporter.escapeCsvField(input)
        assertEquals(expected, result)
    }

    @Test
    fun escapeCsvField_containsNewlines_wrappedInQuotes() {
        val input = "دفعة أولى\nمتبقي قسطين"
        val result = csvExporter.escapeCsvField(input)
        assertEquals("\"$input\"", result)
    }

    @Test
    fun exportTransactions_formatsTransactionRowCorrectly() {
        val tx = TransactionEntity(
            id = 42L,
            amount = 150.50,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            title = "غداء عمل",
            note = "مع الزملاء",
            date = 1788600000000L
        )

        val categoryMap = mapOf(1L to "أكل ومشروبات")
        val result = csvExporter.exportTransactions(listOf(tx), categoryMap)

        assertTrue(result.contains("42"))
        assertTrue(result.contains("غداء عمل"))
        assertTrue(result.contains("مصروف"))
        assertTrue(result.contains("150.50"))
        assertTrue(result.contains("أكل ومشروبات"))
        assertTrue(result.contains("مع الزملاء"))
    }
}
