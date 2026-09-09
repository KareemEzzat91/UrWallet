package com.example.urwallet.features.challenges.domain.usecase

import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.features.challenges.domain.model.ChallengePreset
import javax.inject.Inject

class GetChallengePresetsUseCase @Inject constructor() {

    operator fun invoke(): List<ChallengePreset> {
        return listOf(
            ChallengePreset(
                id = "preset_no_cafe_7d",
                title = "أسبوع بلا كافيهات ومطاعم ☕",
                description = "امتنع عن نفقات المطاعم والمقاهي لمدة 7 أيام متتالية واصنع قهوتك ووجباتك بنفسك.",
                type = ChallengeType.NO_SPENDING,
                targetDays = 7,
                categoryIcon = "ic_food",
                categoryName = "أكل ومشروبات",
                difficulty = "متوسط",
                durationText = "7 أيام"
            ),
            ChallengePreset(
                id = "preset_zero_spend_1d",
                title = "يوم بلا مصاريف 🚫",
                description = "تحدَّ نفسك بقضاء 24 ساعة كاملة دون دفع أي مصروفات غير ضرورية إطلاقاً.",
                type = ChallengeType.NO_SPENDING,
                targetDays = 1,
                categoryIcon = "ic_other",
                categoryName = null,
                difficulty = "سهل",
                durationText = "يوم واحد"
            ),
            ChallengePreset(
                id = "preset_save_1000_30d",
                title = "ادخار 1,000 ج.م في 30 يوم 💰",
                description = "خصص مبالغ ادخار منتظمة في أهدافك المالية للوصول إلى 1,000 ج.م قبل نهاية الشهر.",
                type = ChallengeType.SAVE_AMOUNT,
                targetDays = 30,
                targetAmount = 1000.0,
                categoryIcon = "ic_bonus",
                difficulty = "متقدم",
                durationText = "30 يوماً"
            ),
            ChallengePreset(
                id = "preset_smart_shopping_14d",
                title = "تسوق اقتصادي وذكي 🛒",
                description = "احصر كافة مصروفات التسوق والكماليات بسقف أقصى لا يتجاوز 500 ج.م خلال أسبوعين.",
                type = ChallengeType.REDUCE_CATEGORY,
                targetDays = 14,
                targetAmount = 500.0,
                categoryIcon = "ic_shopping",
                categoryName = "تسوق",
                difficulty = "متوسط",
                durationText = "14 يوماً"
            ),
            ChallengePreset(
                id = "preset_calm_weekend_2d",
                title = "تحدي نهاية الأسبوع الهادئ 🌴",
                description = "استمتع بعطلة نهاية أسبوع ممتعة ودافئة في المنزل والحدائق دون أي إنفاق مالي.",
                type = ChallengeType.NO_SPENDING,
                targetDays = 2,
                categoryIcon = "ic_entertainment",
                categoryName = null,
                difficulty = "سهل",
                durationText = "يومان"
            )
        )
    }
}
