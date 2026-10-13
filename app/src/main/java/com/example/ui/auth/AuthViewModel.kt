package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FgTimeRepository
import com.example.model.UserProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: FirebaseUser, val profile: UserProfile?) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val repository: FgTimeRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.value = user
        if (user != null) {
            checkAndSyncUserProfile(user)
        } else {
            _uiState.value = AuthUiState.Idle
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        val initialUser = auth.currentUser
        if (initialUser != null) {
            checkAndSyncUserProfile(initialUser)
        }
    }

    private fun checkAndSyncUserProfile(user: FirebaseUser) {
        viewModelScope.launch {
            try {
                val emailHandle = user.email?.substringBefore("@")?.lowercase()?.replace(".", "_") ?: "user_${user.uid.take(5)}"
                val fallbackProfile = UserProfile(
                    userId = user.uid,
                    displayName = user.displayName ?: "FG Member",
                    username = emailHandle,
                    bio = "Spreading growth and ideas on FG TIME 🌱",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    createdAt = System.currentTimeMillis()
                )
                repository.observeUserProfile(user.uid).collect { existing ->
                    if (existing == null) {
                        repository.saveUserProfile(fallbackProfile)
                        _uiState.value = AuthUiState.Authenticated(user, fallbackProfile)
                    } else {
                        _uiState.value = AuthUiState.Authenticated(user, existing)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error syncing profile", e)
                _uiState.value = AuthUiState.Authenticated(user, null)
            }
        }
    }

    fun signInWithGoogle(context: Context, serverClientId: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build()

                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        checkAndSyncUserProfile(user)
                    }
                } else {
                    _uiState.value = AuthUiState.Error("Unexpected credential type received")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.i("AuthViewModel", "Sign in cancelled by user")
                _uiState.value = AuthUiState.Idle
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "Credential manager failure", e)
                _uiState.value = AuthUiState.Error("Google Sign-In failed: ${e.message ?: "Please try again."}")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Authentication exception", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Authentication failed")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.value = AuthUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }
}
