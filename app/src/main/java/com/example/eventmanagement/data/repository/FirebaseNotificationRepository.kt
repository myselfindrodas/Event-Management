package com.example.eventmanagement.data.repository

import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.time.AppClock
import com.example.eventmanagement.data.firebase.mapFirebaseException
import com.example.eventmanagement.domain.repository.NotificationRepository
import com.example.eventmanagement.domain.session.UserSession
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseNotificationRepository @Inject constructor(
    private val messaging: FirebaseMessaging,
    private val firestore: FirebaseFirestore,
    private val userSession: UserSession,
    private val clock: AppClock
) : NotificationRepository {

    override suspend fun subscribeToReminders(): Resource<Unit> {
        return try {
            messaging.subscribeToTopic(GLOBAL_ANNOUNCEMENTS_TOPIC).await()
            Resource.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    override suspend fun unsubscribeFromReminders(): Resource<Unit> {
        return try {
            messaging.unsubscribeFromTopic(GLOBAL_ANNOUNCEMENTS_TOPIC).await()
            Resource.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    override suspend fun getToken(): Resource<String> {
        return try {
            val token = messaging.token.await()
            if (token.isNullOrBlank()) Resource.Error(AppError.Unknown())
            else Resource.Success(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    override suspend fun registerToken(token: String): Resource<Unit> {
        return try {
            val userId = userSession.currentUserId() ?: return Resource.Error(AppError.Unauthorized)
            if (token.isBlank()) return Resource.Error(AppError.Unknown())
            val nowInstant = clock.now()
            val now = Timestamp(nowInstant.epochSecond, nowInstant.nano)
            firestore.collection("users")
                .document(userId)
                .collection("devices")
                .document(tokenToDocId(token))
                .set(
                    mapOf(
                        "token" to token,
                        "platform" to "android",
                        "updatedAt" to now
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    override suspend fun unregisterCurrentToken(): Resource<Unit> {
        return try {
            val userId = userSession.currentUserId() ?: return Resource.Success(Unit)
            val token = runCatching { messaging.token.await() }.getOrNull()
            if (!token.isNullOrBlank()) {
                firestore.collection("users")
                    .document(userId)
                    .collection("devices")
                    .document(tokenToDocId(token))
                    .delete()
                    .await()
            }
            Resource.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    private fun tokenToDocId(token: String): String =
        token.replace("/", "_")

    companion object {
        const val GLOBAL_ANNOUNCEMENTS_TOPIC = "event_announcements"
    }
}
