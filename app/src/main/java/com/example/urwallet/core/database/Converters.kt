package com.example.urwallet.core.database

import androidx.room.TypeConverter
import com.example.urwallet.core.common.BudgetStatus
import com.example.urwallet.core.common.CategoryType
import com.example.urwallet.core.common.ChallengeType
import com.example.urwallet.core.common.Frequency
import com.example.urwallet.core.common.GoalPaceMode
import com.example.urwallet.core.common.TransactionType

class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? =
        value?.let { enumValueOf<TransactionType>(it) }

    @TypeConverter
    fun fromCategoryType(value: CategoryType?): String? = value?.name

    @TypeConverter
    fun toCategoryType(value: String?): CategoryType? =
        value?.let { enumValueOf<CategoryType>(it) }

    @TypeConverter
    fun fromGoalPaceMode(value: GoalPaceMode?): String? = value?.name

    @TypeConverter
    fun toGoalPaceMode(value: String?): GoalPaceMode? =
        value?.let { enumValueOf<GoalPaceMode>(it) }

    @TypeConverter
    fun fromBudgetStatus(value: BudgetStatus?): String? = value?.name

    @TypeConverter
    fun toBudgetStatus(value: String?): BudgetStatus? =
        value?.let { enumValueOf<BudgetStatus>(it) }

    @TypeConverter
    fun fromFrequency(value: Frequency?): String? = value?.name

    @TypeConverter
    fun toFrequency(value: String?): Frequency? =
        value?.let { enumValueOf<Frequency>(it) }

    @TypeConverter
    fun fromChallengeType(value: ChallengeType?): String? = value?.name

    @TypeConverter
    fun toChallengeType(value: String?): ChallengeType? =
        value?.let { enumValueOf<ChallengeType>(it) }
}
