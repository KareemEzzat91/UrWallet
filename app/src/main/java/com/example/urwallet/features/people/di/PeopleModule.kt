package com.example.urwallet.features.people.di

import com.example.urwallet.features.people.data.repository.PeopleRepositoryImpl
import com.example.urwallet.features.people.domain.repository.PeopleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PeopleModule {

    @Binds
    @Singleton
    abstract fun bindPeopleRepository(
        impl: PeopleRepositoryImpl
    ): PeopleRepository
}
