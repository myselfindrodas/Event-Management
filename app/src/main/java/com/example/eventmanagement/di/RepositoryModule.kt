package com.example.eventmanagement.di

import com.example.eventmanagement.data.repository.FirebaseAuthRepository
import com.example.eventmanagement.data.repository.FirebaseEventRepository
import com.example.eventmanagement.data.repository.FirebaseNotificationRepository
import com.example.eventmanagement.domain.repository.AuthRepository
import com.example.eventmanagement.domain.repository.EventRepository
import com.example.eventmanagement.domain.repository.NotificationRepository
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
    abstract fun bindAuthRepository(implementation: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(implementation: FirebaseEventRepository): EventRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        implementation: FirebaseNotificationRepository
    ): NotificationRepository
}
