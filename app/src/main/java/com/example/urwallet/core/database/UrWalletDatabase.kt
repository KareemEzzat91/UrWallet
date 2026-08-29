package com.example.urwallet.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.urwallet.core.common.Constants
import com.example.urwallet.features.budgets.data.dao.BudgetDao
import com.example.urwallet.features.budgets.data.entity.BudgetEntity
import com.example.urwallet.features.challenges.data.dao.ChallengeDao
import com.example.urwallet.features.challenges.data.entity.ChallengeEntity
import com.example.urwallet.features.goals.data.dao.GoalContributionDao
import com.example.urwallet.features.goals.data.dao.GoalDao
import com.example.urwallet.features.goals.data.entity.GoalContributionEntity
import com.example.urwallet.features.goals.data.entity.GoalEntity
import com.example.urwallet.features.more.data.dao.RecurringTransactionDao
import com.example.urwallet.features.more.data.entity.RecurringTransactionEntity
import com.example.urwallet.features.transactions.data.dao.CategoryDao
import com.example.urwallet.features.transactions.data.dao.TransactionDao
import com.example.urwallet.features.transactions.data.entity.CategoryEntity
import com.example.urwallet.features.transactions.data.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        ChallengeEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UrWalletDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun goalDao(): GoalDao
    abstract fun goalContributionDao(): GoalContributionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun challengeDao(): ChallengeDao

    companion object {
        @Volatile
        private var INSTANCE: UrWalletDatabase? = null

        fun getInstance(context: Context): UrWalletDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): UrWalletDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                UrWalletDatabase::class.java,
                Constants.DATABASE_NAME
            )
                .addCallback(DatabaseCallback { INSTANCE ?: buildDatabase(context) })
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
