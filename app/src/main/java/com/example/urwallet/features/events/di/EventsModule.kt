package com.example.urwallet.features.events.di

import com.example.urwallet.features.events.data.repository.ContactResolutionRepositoryImpl
import com.example.urwallet.features.events.data.repository.FinancialEventRepositoryImpl
import com.example.urwallet.features.events.domain.repository.ContactResolutionRepository
import com.example.urwallet.features.events.domain.repository.FinancialEventRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EventsModule {

    @Binds
    @Singleton
    abstract fun bindFinancialEventRepository(
        impl: FinancialEventRepositoryImpl
    ): FinancialEventRepository

    @Binds
    @Singleton
    abstract fun bindContactResolutionRepository(
        impl: ContactResolutionRepositoryImpl
    ): ContactResolutionRepository
}
