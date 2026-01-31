package com.example.castlegame.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute

    fun setPendingRoute(route: String) {
        _pendingRoute.value = route
    }

    fun consumePendingRoute(): String? {
        val route = _pendingRoute.value
        _pendingRoute.value = null
        return route
    }

    private val _isAuthResolved = MutableStateFlow(false)
    val isAuthResolved: StateFlow<Boolean> = _isAuthResolved

    private val auth = FirebaseAuth.getInstance()

    /* ---------------------------------------------------
       🔐 SESSION STATE (Auth guard / auto-login)
     --------------------------------------------------- */

    val user: StateFlow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser)
            _isAuthResolved.value = true
        }

        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = auth.currentUser
    )

    fun logout() {
        auth.signOut()
    }

    /* ---------------------------------------------------
       🔁 ACTION RESULT STATE (login / register UI)
     --------------------------------------------------- */

    private val _authState = MutableStateFlow<AuthResultState>(AuthResultState.Idle)
    val authState: StateFlow<AuthResultState> = _authState

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value =
                AuthResultState.Error("Please enter both email and password.")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _authState.value = AuthResultState.Success
            }
            .addOnFailureListener {
                _authState.value =
                    AuthResultState.Error(it.localizedMessage ?: "Unknown error")
            }
    }

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _authState.value = AuthResultState.Success
            }
            .addOnFailureListener {
                _authState.value =
                    AuthResultState.Error(it.localizedMessage ?: "Unknown error")
            }
    }

    fun resetAuthState() {
        _authState.value = AuthResultState.Idle
    }
}

/* ---------------------------------------------------
   UI RESULT STATE
 --------------------------------------------------- */

sealed class AuthResultState {
    object Idle : AuthResultState()
    object Success : AuthResultState()
    data class Error(val message: String) : AuthResultState()
}

