package com.example.urwallet.core.di

import android.content.Context
import com.example.urwallet.core.database.UrWalletDatabase
import com.example.urwallet.features.budgets.data.dao.BudgetDao
import com.example.urwallet.features.challenges.data.dao.ChallengeDao
import com.example.urwallet.features.goals.data.dao.GoalContributionDao
import com.example.urwallet.features.goals.data.dao.GoalDao
import com.example.urwallet.features.more.data.dao.RecurringTransactionDao
import com.example.urwallet.features.transactions.data.dao.CategoryDao
import com.example.urwallet.features.transactions.data.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): UrWalletDatabase {
        return UrWalletDatabase.getInstance(context)
    }

    @Provides
    fun provideGoalDao(database: UrWalletDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideGoalContributionDao(database: UrWalletDatabase): GoalContributionDao {
        return database.goalContributionDao()
    }

    @Provides
    fun provideTransactionDao(database: UrWalletDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: UrWalletDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideBudgetDao(database: UrWalletDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    fun provideChallengeDao(database: UrWalletDatabase): ChallengeDao {
        return database.challengeDao()
    }

    @Provides
    fun provideRecurringTransactionDao(database: UrWalletDatabase): RecurringTransactionDao {
        return database.recurringTransactionDao()
    }
}
