package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthResult
import com.example.data.repository.AuthenticationRepository
import com.example.data.repository.AuthenticationRepositoryImpl
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthenticationRepository = AuthenticationRepositoryImpl()
) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUserFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = authRepository.currentUser
        )

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.signInWithGoogle(context)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthUiState.Success
                }
                is AuthResult.Cancelled -> {
                    _uiState.value = AuthUiState.Idle
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.signInWithEmail(email, pass)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthUiState.Success
                }
                is AuthResult.Cancelled -> {
                    _uiState.value = AuthUiState.Idle
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, passConfirm: String) {
        if (pass != passConfirm) {
            _uiState.value = AuthUiState.Error("Passwords do not match")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = authRepository.signUpWithEmail(email, pass)) {
                is AuthResult.Success -> {
                    _uiState.value = AuthUiState.Success
                }
                is AuthResult.Cancelled -> {
                    _uiState.value = AuthUiState.Idle
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState.Idle
    }
}
