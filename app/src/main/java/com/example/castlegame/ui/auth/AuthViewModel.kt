package com.example.castlegame.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    /* ---------------------------------------------------
       🔐 SESSION STATE
     --------------------------------------------------- */

    val user: StateFlow<FirebaseUser?> = repository.authStateFlow()
        .onEach { _isAuthResolved.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.currentUser
        )

    private val _isAuthResolved = MutableStateFlow(false)
    val isAuthResolved: StateFlow<Boolean> = _isAuthResolved.asStateFlow()

    /**
     * Stable key representing the current user identity.
     * Collected with plain collectAsState() — no extra dependency needed.
     *
     * Changes on:  login  ("anonymous" → uid)
     *              logout (uid → "anonymous")
     *              account switch (uid_A → uid_B)
     *
     * Used in AppNavigation's key(sessionKey) block to force GameViewModel
     * to be recreated from scratch whenever the user identity changes.
     */
    val userSessionKey: StateFlow<String> = user
        .map { it?.uid ?: "anonymous" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.currentUser?.uid ?: "anonymous"
        )

    /* ---------------------------------------------------
       🔀 PENDING ROUTE
     --------------------------------------------------- */

    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    fun setPendingRoute(route: String) { _pendingRoute.value = route }

    fun consumePendingRoute(): String? {
        val route = _pendingRoute.value
        _pendingRoute.value = null
        return route
    }

    /* ---------------------------------------------------
       ✉️ ACTION RESULT STATE
     --------------------------------------------------- */

    private val _authState = MutableStateFlow<AuthResultState>(AuthResultState.Idle)
    val authState: StateFlow<AuthResultState> = _authState.asStateFlow()

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthResultState.Error("Please enter both email and password.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthResultState.Error("Password must be at least 6 characters.")
            return
        }
        _authState.value = AuthResultState.Loading
        viewModelScope.launch {
            val result = repository.register(email, password)
            _authState.value = result.fold(
                onSuccess = { AuthResultState.Success },
                onFailure = { AuthResultState.Error(it.localizedMessage ?: "Registration failed. Please try again.") }
            )
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthResultState.Error("Please enter both email and password.")
            return
        }
        _authState.value = AuthResultState.Loading
        viewModelScope.launch {
            val result = repository.login(email, password)
            _authState.value = result.fold(
                onSuccess = { AuthResultState.Success },
                onFailure = { AuthResultState.Error(it.localizedMessage ?: "Login failed. Please check your credentials.") }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        Log.d("AUTH", "Google token received")
        _authState.value = AuthResultState.Loading
        viewModelScope.launch {
            val result = repository.signInWithGoogle(idToken)
            Log.d("AUTH", "Firebase login result: $result")
            _authState.value = result.fold(
                onSuccess = { AuthResultState.Success },
                onFailure = { AuthResultState.Error(it.message ?: "Google login failed") }
            )
        }
    }

    fun loginWithFacebook(token: String) {
        _authState.value = AuthResultState.Loading
        viewModelScope.launch {
            val result = repository.signInWithFacebook(token)
            _authState.value = result.fold(
                onSuccess = { AuthResultState.Success },
                onFailure = { AuthResultState.Error(it.message ?: "Facebook login failed") }
            )
        }
    }

    fun logout() {
        repository.logout()
        resetAuthState()
    }

    fun resetAuthState() {
        _authState.value = AuthResultState.Idle
    }
}

/* ---------------------------------------------------
   UI RESULT STATE
 --------------------------------------------------- */

sealed class AuthResultState {
    object Idle    : AuthResultState()
    object Loading : AuthResultState()
    object Success : AuthResultState()
    data class Error(val message: String) : AuthResultState()
}


/*
package com.example.castlegame.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    */
/* ---------------------------------------------------
       🔐 SESSION STATE (Auth guard / auto-login)
     --------------------------------------------------- *//*


    */
/**
     * Tracks the currently authenticated user
     * Null when logged out, FirebaseUser when logged in
     *//*

    val user: StateFlow<FirebaseUser?> = repository.authStateFlow()
        .onEach { _isAuthResolved.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.currentUser
        )

    */
/**
     * Indicates whether initial auth state has been resolved
     * Useful for splash screens to know when to proceed
     *//*

    private val _isAuthResolved = MutableStateFlow(false)
    val isAuthResolved: StateFlow<Boolean> = _isAuthResolved.asStateFlow()

    val userSessionKey: StateFlow<String> = user
        .map { it?.uid ?: "anonymous" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.currentUser?.uid ?: "anonymous"
        )


    */
/* ---------------------------------------------------
       🔀 PENDING ROUTE (Deep linking / navigation)
     --------------------------------------------------- *//*


    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    fun setPendingRoute(route: String) {
        _pendingRoute.value = route
    }

    fun consumePendingRoute(): String? {
        val route = _pendingRoute.value
        _pendingRoute.value = null
        return route
    }

    */
/* ---------------------------------------------------
       ✉️ ACTION RESULT STATE (login / register UI)
     --------------------------------------------------- *//*


    private val _authState = MutableStateFlow<AuthResultState>(AuthResultState.Idle)
    val authState: StateFlow<AuthResultState> = _authState.asStateFlow()

    */
/**
     * Registers a new user with email and password
     * Updates authState with Loading -> Success/Error
     *//*

    fun register(email: String, password: String) {
        // Input validation
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthResultState.Error("Please enter both email and password.")
            return
        }

        if (password.length < 6) {
            _authState.value = AuthResultState.Error("Password must be at least 6 characters.")
            return
        }

        // Set loading state
        _authState.value = AuthResultState.Loading

        viewModelScope.launch {
            val result = repository.register(email, password)

            _authState.value = result.fold(
                onSuccess = { AuthResultState.Success },
                onFailure = { error ->
                    AuthResultState.Error(
                        error.localizedMessage ?: "Registration failed. Please try again."
                    )
                }
            )
        }
    }

    */
/**
     * Logs in an existing user with email and password
     * Updates authState with Loading -> Success/Error
     *//*

    fun login(email: String, password: String) {
        // Input validation
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthResultState.Error("Please enter both email and password.")
            return
        }

        // Set loading state
        _authState.value = AuthResultState.Loading

        viewModelScope.launch {
            val result = repository.login(email, password)
            _authState.value = result.fold(
                onSuccess = { AuthResultState.Success },
                onFailure = { AuthResultState.Error(it.localizedMessage ?: "Login failed. Please check your credentials.") }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {

        Log.d("AUTH", "Google token received")

        _authState.value = AuthResultState.Loading

        viewModelScope.launch {

            val result = repository.signInWithGoogle(idToken)

            Log.d("AUTH", "Firebase login result: $result")

            _authState.value = result.fold(
                onSuccess = { AuthResultState.Success },
                onFailure = { AuthResultState.Error(it.message ?: "Google login failed") }
            )
        }
    }

    fun loginWithFacebook(token: String) {

        _authState.value = AuthResultState.Loading

        viewModelScope.launch {

            val result = repository.signInWithFacebook(token)

            _authState.value = result.fold(
                onSuccess = { AuthResultState.Success },
                onFailure = { AuthResultState.Error(it.message ?: "Facebook login failed") }
            )
        }
    }


    */
/**
     * Logs out the current user
     *//*

    fun logout() {
        repository.logout()
        resetAuthState()
    }

    */
/**
     * Resets auth state back to Idle
     * Call this when navigating away from login/register screens
     *//*

    fun resetAuthState() {
        _authState.value = AuthResultState.Idle
    }
}

*/
/* ---------------------------------------------------
   UI RESULT STATE
 --------------------------------------------------- *//*


*/
/**
 * Represents the state of authentication actions (login/register)
 *//*

sealed class AuthResultState {
    */
/** Initial state, no action taken *//*

    object Idle : AuthResultState()

    */
/** Action in progress (login/register) *//*

    object Loading : AuthResultState()

    */
/** Action succeeded *//*

    object Success : AuthResultState()

    */
/** Action failed with error message *//*

    data class Error(val message: String) : AuthResultState()
}
*/
