package com.example.urwallet.core.di

import com.example.urwallet.features.budgets.data.repository.BudgetRepositoryImpl
import com.example.urwallet.features.budgets.domain.repository.BudgetRepository
import com.example.urwallet.features.challenges.data.repository.ChallengeRepositoryImpl
import com.example.urwallet.features.challenges.domain.repository.ChallengeRepository
import com.example.urwallet.features.goals.data.repository.GoalRepositoryImpl
import com.example.urwallet.features.goals.domain.repository.GoalRepository
import com.example.urwallet.features.more.data.repository.RecurringRepositoryImpl
import com.example.urwallet.features.more.domain.repository.RecurringRepository
import com.example.urwallet.features.transactions.data.repository.TransactionRepositoryImpl
import com.example.urwallet.features.transactions.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        impl: GoalRepositoryImpl
    ): GoalRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindChallengeRepository(
        impl: ChallengeRepositoryImpl
    ): ChallengeRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRepository(
        impl: RecurringRepositoryImpl
    ): RecurringRepository
}
