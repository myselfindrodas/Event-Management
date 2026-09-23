package com.example.eventmanagement.data.repository

import com.example.eventmanagement.core.error.AppError
import com.example.eventmanagement.core.error.Resource
import com.example.eventmanagement.data.firebase.mapFirebaseException
import com.example.eventmanagement.domain.model.AuthUser
import com.example.eventmanagement.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = auth.currentUser?.toDomain()

    override val currentUserEmail: String?
        get() = auth.currentUser?.email

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override fun observeAuthState(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toDomain())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signUp(email: String, password: String): Resource<AuthUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user?.toDomain()
            if (user != null) Resource.Success(user)
            else Resource.Error(AppError.Unknown())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    override suspend fun login(email: String, password: String): Resource<AuthUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user?.toDomain()
            if (user != null) Resource.Success(user)
            else Resource.Error(AppError.Unknown())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    override suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Resource.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(mapFirebaseException(e))
        }
    }

    override fun logout() {
        auth.signOut()
    }

    private fun FirebaseUser.toDomain() = AuthUser(uid = uid, email = email)
}
