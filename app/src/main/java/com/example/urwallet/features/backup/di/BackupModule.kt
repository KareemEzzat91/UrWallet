package com.example.urwallet.features.backup.di

import com.example.urwallet.features.backup.data.parser.BackupJsonParser
import com.example.urwallet.features.backup.data.parser.CsvExporter
import com.example.urwallet.features.backup.data.repository.BackupRepositoryImpl
import com.example.urwallet.features.backup.domain.repository.BackupRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupBindingModule {

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: BackupRepositoryImpl
    ): BackupRepository
}

@Module
@InstallIn(SingletonComponent::class)
object BackupProvidersModule {

    @Provides
    @Singleton
    fun provideBackupJsonParser(): BackupJsonParser = BackupJsonParser()

    @Provides
    @Singleton
    fun provideCsvExporter(): CsvExporter = CsvExporter()
}
