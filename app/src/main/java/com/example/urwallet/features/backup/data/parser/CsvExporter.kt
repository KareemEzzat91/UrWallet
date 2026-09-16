package com.example.urwallet.features.backup.data.parser

import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.transactions.data.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter {

    companion object {
        const val UTF8_BOM = "\uFEFF"
        const val HEADERS = "المعرف,العنوان,النوع,المبلغ,التصنيف,التاريخ,الملاحظات"
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun exportTransactions(
        transactions: List<TransactionEntity>,
        categoryNameMap: Map<Long, String> = emptyMap()
    ): String {
        val builder = StringBuilder()
        builder.append(UTF8_BOM)
        builder.append(HEADERS).append("\n")

        for (tx in transactions) {
            val typeStr = when (tx.type) {
                TransactionType.INCOME -> "دخل"
                TransactionType.EXPENSE -> "مصروف"
            }
            val categoryName = categoryNameMap[tx.categoryId] ?: "غير محدد"
            val formattedDate = dateFormat.format(Date(tx.date))
            val note = tx.note ?: ""

            val line = listOf(
                escapeCsvField(tx.id.toString()),
                escapeCsvField(tx.title),
                escapeCsvField(typeStr),
                escapeCsvField(String.format(Locale.US, "%.2f", tx.amount)),
                escapeCsvField(categoryName),
                escapeCsvField(formattedDate),
                escapeCsvField(note)
            ).joinToString(",")

            builder.append(line).append("\n")
        }

        return builder.toString()
    }

    internal fun escapeCsvField(field: String): String {
        val containsSpecialChars = field.contains(",") ||
                field.contains("\"") ||
                field.contains("\n") ||
                field.contains("\r")

        return if (containsSpecialChars) {
            val escaped = field.replace("\"", "\"\"")
            "\"$escaped\""
        } else {
            field
        }
    }
}
