package com.example.eventmanagement.data.repository

import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.core.time.AppClock
import com.example.eventmanagement.data.dto.EventDto
import com.example.eventmanagement.data.firebase.mapFirebaseException
import com.example.eventmanagement.data.mapper.toDomain
import com.example.eventmanagement.data.mapper.toWriteMap
import com.example.eventmanagement.di.ApplicationScope
import com.example.eventmanagement.domain.model.Event
import com.example.eventmanagement.domain.repository.EventRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirebaseEventRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val clock: AppClock,
    @ApplicationScope applicationScope: CoroutineScope
) : EventRepository {

    private val sharedEvents: Flow<Resource<List<Event>>> = authState()
        .flatMapLatest { uid ->
            if (uid == null) flowOf(Resource.Error(AppError.Unauthorized))
            else snapshotFlow(uid)
        }
        .shareIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(5_000),
            replay = 1
        )

    override fun observeEvents(): Flow<Resource<List<Event>>> = sharedEvents

    override suspend fun addEvent(event: Event): Resource<String> {
        return try {
            val userId = requireUserId()
            val instant = clock.now()
            val now = Timestamp(instant.epochSecond, instant.nano)
            val data = event.copy(userId = userId).toWriteMap(createdAt = now, updatedAt = now)
            val ref = eventsCollection(userId).add(data).await()
            Resource.Success(ref.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnauthorizedException) {
            Resource.Error(AppError.Unauthorized)
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    override suspend fun updateEvent(event: Event): Resource<Unit> {
        return try {
            val userId = requireUserId()
            if (event.id.isBlank()) return Resource.Error(AppError.InvalidEventId)
            val instant = clock.now()
            val now = Timestamp(instant.epochSecond, instant.nano)
            val data = event.copy(userId = userId)
                .toWriteMap(createdAt = null, updatedAt = now)
                .toMutableMap()
            data.remove("createdAt")
            eventsCollection(userId).document(event.id).update(data).await()
            Resource.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnauthorizedException) {
            Resource.Error(AppError.Unauthorized)
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    override suspend fun deleteEvent(eventId: String): Resource<Unit> {
        return try {
            val userId = requireUserId()
            if (eventId.isBlank()) return Resource.Error(AppError.InvalidEventId)
            eventsCollection(userId).document(eventId).delete().await()
            Resource.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnauthorizedException) {
            Resource.Error(AppError.Unauthorized)
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    override suspend fun getEvent(eventId: String): Resource<Event> {
        return try {
            val userId = requireUserId()
            if (eventId.isBlank()) return Resource.Error(AppError.InvalidEventId)
            val snapshot = eventsCollection(userId).document(eventId).get().await()
            val event = snapshot.toObject(EventDto::class.java)?.toDomain(snapshot.id)
            if (event != null) Resource.Success(event)
            else Resource.Error(AppError.NotFound)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnauthorizedException) {
            Resource.Error(AppError.Unauthorized)
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    private fun snapshotFlow(userId: String): Flow<Resource<List<Event>>> = callbackFlow {
        trySend(Resource.Loading)
        val registration = eventsCollection(userId)
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(mapFirebaseException(error)))
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(EventDto::class.java)?.toDomain(doc.id)
                }.orEmpty()
                trySend(Resource.Success(events))
            }
        awaitClose { registration.remove() }
    }

    private fun authState(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun eventsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("events")

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw UnauthorizedException()

    private class UnauthorizedException : Exception()
}
