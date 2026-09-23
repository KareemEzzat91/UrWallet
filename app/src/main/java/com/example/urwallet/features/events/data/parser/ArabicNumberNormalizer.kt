package com.example.urwallet.features.events.data.parser

object ArabicNumberNormalizer {

    private val ARABIC_INDIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /**
     * Converts Eastern Arabic (Arabic-Indic) numerals (٠-٩) to Western Arabic (0-9).
     */
    fun normalizeDigits(input: String): String {
        val builder = java.lang.StringBuilder(input.length)
        for (ch in input) {
            val idx = ARABIC_INDIC_DIGITS.indexOf(ch)
            if (idx != -1) {
                builder.append(('0' + idx))
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    /**
     * Extracts and parses a Double amount from normalized numeric string.
     * Handles thousand separators (commas/spaces) and Arabic decimal marks.
     */
    fun parseAmount(amountStr: String): Double? {
        val normalized = normalizeDigits(amountStr.trim())
            .replace(" ", "")
            .replace("٫", ".")
            .replace("٬", ",")

        // If string contains both comma and dot (e.g., 1,500.50 or 1.500,50)
        val cleaned = if (normalized.contains(",") && normalized.contains(".")) {
            if (normalized.lastIndexOf(",") > normalized.lastIndexOf(".")) {
                // European/Arabic style: 1.500,50 -> 1500.50
                normalized.replace(".", "").replace(",", ".")
            } else {
                // Standard style: 1,500.50 -> 1500.50
                normalized.replace(",", "")
            }
        } else if (normalized.contains(",")) {
            // Check if comma is decimal (followed by 1 or 2 digits at the end) or thousands separator (followed by 3 digits)
            val parts = normalized.split(",")
            if (parts.size == 2 && parts[1].length in 1..2) {
                normalized.replace(",", ".")
            } else {
                normalized.replace(",", "")
            }
        } else {
            normalized
        }

        return cleaned.toDoubleOrNull()
    }
}
