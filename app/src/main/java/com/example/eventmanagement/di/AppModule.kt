package com.example.eventmanagement.di

import com.example.eventmanagement.core.log.AndroidAppLogger
import com.example.eventmanagement.core.log.AppLogger
import com.example.eventmanagement.core.time.AppClock
import com.example.eventmanagement.core.time.SystemAppClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindsModule {

    @Binds
    @Singleton
    abstract fun bindAppClock(implementation: SystemAppClock): AppClock

    @Binds
    @Singleton
    abstract fun bindAppLogger(implementation: AndroidAppLogger): AppLogger
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
