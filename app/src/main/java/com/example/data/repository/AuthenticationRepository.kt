package com.example.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed interface AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : AuthResult<Nothing>
    data object Cancelled : AuthResult<Nothing>
}

interface AuthenticationRepository {
    val currentUserFlow: Flow<FirebaseUser?>
    val currentUser: FirebaseUser?

    suspend fun signInWithGoogle(context: Context): AuthResult<FirebaseUser>
    suspend fun signInWithEmail(email: String, pass: String): AuthResult<FirebaseUser>
    suspend fun signUpWithEmail(email: String, pass: String): AuthResult<FirebaseUser>
    fun signOut(): AuthResult<Unit>
}

open class AuthenticationRepositoryImpl(
    customAuth: FirebaseAuth? = null
) : AuthenticationRepository {

    private val auth: FirebaseAuth by lazy {
        customAuth ?: Firebase.auth
    }

    override val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override suspend fun signInWithGoogle(context: Context): AuthResult<FirebaseUser> {
        return try {
            val serverClientId = context.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Authenticated user was null after Google sign-in")
                }
            } else {
                AuthResult.Error("Unsupported credential type received from Credential Manager")
            }
        } catch (_: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Google Sign-In failed", e)
        }
    }

    override suspend fun signInWithEmail(email: String, pass: String): AuthResult<FirebaseUser> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            return AuthResult.Error("Please enter both email and password")
        }
        return try {
            val credential = EmailAuthProvider.getCredential(trimmedEmail, pass)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("User record was not found after sign-in")
            }
        } catch (e: Exception) {
            val rawMsg = e.localizedMessage ?: "Email sign-in failed"
            val userFriendlyMsg = if (rawMsg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
                rawMsg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true)
            ) {
                "Email/Password provider is not yet enabled in the Firebase Console. Please sign in with Google or enable Email/Password under Authentication > Sign-in method in Firebase Console."
            } else {
                rawMsg
            }
            AuthResult.Error(userFriendlyMsg, e)
        }
    }

    override suspend fun signUpWithEmail(email: String, pass: String): AuthResult<FirebaseUser> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || pass.isBlank()) {
            return AuthResult.Error("Please enter all required fields")
        }
        if (pass.length < 6) {
            return AuthResult.Error("Password must be at least 6 characters")
        }
        return try {
            val result = auth.createUserWithEmailAndPassword(trimmedEmail, pass).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("User record was not created")
            }
        } catch (e: Exception) {
            val rawMsg = e.localizedMessage ?: "Account creation failed"
            val userFriendlyMsg = if (rawMsg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
                rawMsg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true)
            ) {
                "Email/Password provider is not yet enabled in the Firebase Console. Please sign in with Google or enable Email/Password under Authentication in Firebase Console."
            } else {
                rawMsg
            }
            AuthResult.Error(userFriendlyMsg, e)
        }
    }

    override fun signOut(): AuthResult<Unit> {
        return try {
            auth.signOut()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e.localizedMessage ?: "Failed to sign out", e)
        }
    }
}
