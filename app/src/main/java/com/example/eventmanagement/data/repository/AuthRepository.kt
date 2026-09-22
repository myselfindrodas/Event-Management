package com.example.eventmanagement.data.repository

import com.example.eventmanagement.data.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun currentUserEmail(): String? = auth.currentUser?.email

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) Resource.Success(user)
            else Resource.Error("Sign up failed. Please try again.")
        } catch (e: Exception) {
            Resource.Error(mapAuthError(e))
        }
    }

    suspend fun login(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) Resource.Success(user)
            else Resource.Error("Login failed. Please try again.")
        } catch (e: Exception) {
            Resource.Error(mapAuthError(e))
        }
    }

    suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(mapAuthError(e))
        }
    }

    fun logout() {
        auth.signOut()
    }

    private fun mapAuthError(e: Exception): String {
        val code = (e as? FirebaseAuthException)?.errorCode
        return when (code) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address."
            "ERROR_WRONG_PASSWORD" -> "Incorrect password."
            "ERROR_USER_NOT_FOUND" -> "No account found with this email."
            "ERROR_USER_DISABLED" -> "This account has been disabled."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists with this email."
            "ERROR_WEAK_PASSWORD" -> "Password should be at least 6 characters."
            "ERROR_INVALID_CREDENTIAL" -> "Invalid email or password."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later."
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection."
            else -> e.localizedMessage ?: "Authentication failed."
        }
    }
}
