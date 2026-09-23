package com.example.urwallet.features.events.domain.usecase

import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.TransactionType
import com.example.urwallet.features.events.domain.model.CategorySuggestion
import com.example.urwallet.features.events.domain.model.CategorySuggestionSource
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import com.example.urwallet.features.transactions.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SuggestCategoryUseCase @Inject constructor(
    private val financialEventRepository: FinancialEventRepository,
    private val categoryRepository: CategoryRepository
) {

    private val groceryKeywords = listOf(
        "carrefour", "كارفور", "lulu", "hyperone", "هايبر وان", "spinneys", "سبينيس",
        "kazyon", "كازيون", "bim", "بيم", "سوبرماركت", "ماركت", "groceries", "market",
        "metro market", "سعودي", "seoudi", "خير زمان", "fresh food", "بقالة"
    )

    private val transportKeywords = listOf(
        "uber", "أوبر", "careem", "كريم", "didi", "ديدي", "indrive", "بنزين", "طاقة",
        "مواصلات", "بترول", "chillout", "وطنية", "مصر للبترول", "gasoline", "fuel",
        "شل", "توتال", "تاكسي", "مترو"
    )

    private val foodKeywords = listOf(
        "mcdonalds", "ماكدونالدز", "kfc", "كنتاكي", "starbucks", "ستاربكس", "costa",
        "مطعم", "كافيه", "قهوة", "food", "burger", "pizza", "shawarma", "شاورما",
        "كباب", "حلواني", "elabd", "العبد", "tsippo", "تسيباس", "b laban", "بلبن"
    )

    private val healthKeywords = listOf(
        "pharmacy", "صيدلية", "صيدليات", "العزبي", "elezaby", "سيف", "19011",
        "مستشفى", "عيادة", "دكتور", "معمل", "lab", "تحاليل", "أشعة"
    )

    private val shoppingKeywords = listOf(
        "amazon", "أمازون", "noon", "نون", "zara", "زارا", "h&m", "lc waikiki",
        "جوميا", "jumia", "ملابس", "أحذية", "مول"
    )

    private val utilitiesKeywords = listOf(
        "vodafone", "فودافون", "orange", "أورانج", "etisalat", "اتصالات", "we", "وي",
        "فواتير", "فاتورة", "كهرباء", "مياه", "غاز", "شحن رصيد", "انترنت", "internet", "te data"
    )

    private val subscriptionKeywords = listOf(
        "netflix", "نتفلكس", "spotify", "سبوتيفاي", "shahid", "شاهد", "playstation",
        "سينما", "cinema", "اشتراك"
    )

    suspend operator fun invoke(
        merchantOrCounterparty: String?,
        rawMessage: String? = null,
        type: TransactionType = TransactionType.EXPENSE
    ): CategorySuggestion? {
        val targetType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        val categories = categoryRepository.getAllCategories().first()
            .filter { it.type == targetType || it.type == CategoryType.BOTH }

        if (categories.isEmpty()) return null

        val cleanMerchant = merchantOrCounterparty?.trim()?.lowercase()

        // 1. Tier 1: Check learned local mapping
        if (!cleanMerchant.isNullOrBlank()) {
            val learned = financialEventRepository.getCategoryMapping(cleanMerchant)
            if (learned != null) {
                val matchedCat = categories.find { it.id == learned.categoryId }
                if (matchedCat != null) {
                    return CategorySuggestion(
                        categoryId = matchedCat.id,
                        categoryName = matchedCat.name,
                        confidence = 0.95f,
                        source = CategorySuggestionSource.LEARNED_MAPPING
                    )
                }
            }
        }

        // 2. Tier 2: Check deterministic keyword dictionary
        val textToSearch = buildString {
            if (!cleanMerchant.isNullOrBlank()) append(cleanMerchant).append(" ")
            if (!rawMessage.isNullOrBlank()) append(rawMessage.lowercase())
        }

        val detectedKeywordGroup = when {
            groceryKeywords.any { textToSearch.contains(it) } -> "بقالة"
            foodKeywords.any { textToSearch.contains(it) } -> "طعام"
            transportKeywords.any { textToSearch.contains(it) } -> "مواصلات"
            healthKeywords.any { textToSearch.contains(it) } -> "صحة"
            shoppingKeywords.any { textToSearch.contains(it) } -> "تسوق"
            utilitiesKeywords.any { textToSearch.contains(it) } -> "فواتير"
            subscriptionKeywords.any { textToSearch.contains(it) } -> "ترفيه"
            else -> null
        }

        if (detectedKeywordGroup != null) {
            // Priority 1: Direct match with detected keyword group (e.g. "بقالة")
            var matchedCat = categories.find { cat ->
                cat.name.contains(detectedKeywordGroup, ignoreCase = true)
            }

            // Priority 2: Semantic fallback (e.g. "بقالة" -> "سوبر" -> "أكل ومشروبات")
            if (matchedCat == null) {
                matchedCat = categories.find { cat ->
                    when (detectedKeywordGroup) {
                        "بقالة" -> cat.name.contains("سوبر", ignoreCase = true) ||
                            cat.name.contains("أكل", ignoreCase = true) ||
                            cat.name.contains("طعام", ignoreCase = true) ||
                            cat.name.contains("تسوق", ignoreCase = true)
                        "طعام" -> cat.name.contains("أكل", ignoreCase = true) ||
                            cat.name.contains("مطاعم", ignoreCase = true) ||
                            cat.name.contains("أغذية", ignoreCase = true)
                        "مواصلات" -> cat.name.contains("تنقل", ignoreCase = true) ||
                            cat.name.contains("سفر", ignoreCase = true)
                        "صحة" -> cat.name.contains("علاج", ignoreCase = true) ||
                            cat.name.contains("أدوية", ignoreCase = true)
                        "فواتير" -> cat.name.contains("خدمات", ignoreCase = true) ||
                            cat.name.contains("اتصالات", ignoreCase = true)
                        else -> false
                    }
                }
            }

            if (matchedCat != null) {
                return CategorySuggestion(
                    categoryId = matchedCat.id,
                    categoryName = matchedCat.name,
                    confidence = 0.85f,
                    source = CategorySuggestionSource.KEYWORD_RULES
                )
            }
        }

        return null
    }
}
