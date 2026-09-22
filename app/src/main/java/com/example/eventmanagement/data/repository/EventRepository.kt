package com.example.eventmanagement.data.repository

import com.example.eventmanagement.data.Resource
import com.example.eventmanagement.data.model.Event
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class EventRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun eventsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("events")

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated.")
    }

    fun observeEvents(): Flow<Resource<List<Event>>> = callbackFlow {
        val userId = try {
            requireUserId()
        } catch (e: Exception) {
            trySend(Resource.Error(e.message ?: "Not authenticated"))
            close()
            return@callbackFlow
        }

        trySend(Resource.Loading)

        val registration = eventsCollection(userId)
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(mapFirestoreError(error)))
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(Resource.Success(events))
            }

        awaitClose { registration.remove() }
    }

    suspend fun addEvent(event: Event): Resource<String> {
        return try {
            val userId = requireUserId()
            val data = event.copy(
                userId = userId,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            val ref = eventsCollection(userId).add(data.toMap()).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Resource.Error(mapFirestoreError(e))
        }
    }

    suspend fun updateEvent(event: Event): Resource<Unit> {
        return try {
            val userId = requireUserId()
            if (event.id.isBlank()) {
                return Resource.Error("Invalid event id.")
            }
            val data = event.copy(
                userId = userId,
                updatedAt = Timestamp.now()
            ).toMap().toMutableMap()
            data.remove("createdAt")
            eventsCollection(userId).document(event.id).update(data).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(mapFirestoreError(e))
        }
    }

    suspend fun deleteEvent(eventId: String): Resource<Unit> {
        return try {
            val userId = requireUserId()
            eventsCollection(userId).document(eventId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(mapFirestoreError(e))
        }
    }

    suspend fun getEvent(eventId: String): Resource<Event> {
        return try {
            val userId = requireUserId()
            val snapshot = eventsCollection(userId).document(eventId).get().await()
            val event = snapshot.toObject(Event::class.java)?.copy(id = snapshot.id)
            if (event != null) Resource.Success(event)
            else Resource.Error("Event not found.")
        } catch (e: Exception) {
            Resource.Error(mapFirestoreError(e))
        }
    }

    private fun mapFirestoreError(e: Exception): String {
        return when ((e as? FirebaseFirestoreException)?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Permission denied. Check Firestore rules."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Firestore unavailable. Showing cached data if available."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "Event not found."
            else -> e.localizedMessage ?: "Something went wrong with Firestore."
        }
    }
}
