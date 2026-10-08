package com.tilworks.castlegame.ui.auth

import android.util.Log
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Deletes all Firestore data owned by [uid] BEFORE the Auth account is
     * deleted. This must run first: once the Auth account is gone,
     * `isOwner(userId)` in the security rules can never be true again for
     * this user, so the client permanently loses write access to clean up
     * after itself (there's no Cloud Function to do it server-side).
     *
     * Best-effort: each step is tried independently so one failure doesn't
     * block the others. Returns the list of step names that failed, so the
     * caller can decide whether to still proceed with Auth deletion and how
     * to inform the user. An empty list means full cleanup succeeded.
     */
    internal suspend fun deleteUserFirestoreData(uid: String): List<String> {
        val userRef = db.collection("users").document(uid)
        val failedSteps = mutableListOf<String>()

        // 1. rankings/{leagueId} — delete whatever exists rather than
        //    assuming all four leagues were played
        try {
            val rankings = userRef.collection("rankings").get().await()
            for (doc in rankings.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Failed to delete rankings for $uid: ${e.message}", e)
            failedSteps.add("rankings")
        }

        // 2. progress/completed_leagues
        try {
            userRef.collection("progress").document("completed_leagues").delete().await()
        } catch (e: Exception) {
            Log.w("AuthRepository", "Failed to delete progress for $uid: ${e.message}", e)
            failedSteps.add("progress")
        }

        // 3. the profile document itself
        try {
            userRef.delete().await()
        } catch (e: Exception) {
            Log.w("AuthRepository", "Failed to delete user doc for $uid: ${e.message}", e)
            failedSteps.add("profile")
        }

        return failedSteps
    }

    /**
     * Gets the current user synchronously
     * Safe to call - returns null if no user is logged in
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Observes authentication state changes
     * Emits whenever user logs in, logs out, or session changes
     */
    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    /**
     * Logs in user with email and password
     * @return Result.success with FirebaseUser or Result.failure with exception
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithFacebook(token: String): Result<FirebaseUser> {
        return try {
            val credential = FacebookAuthProvider.getCredential(token)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registers new user with email and password
     * @return Result.success with FirebaseUser or Result.failure with exception
     */
    suspend fun register(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user
     */
    fun logout() {
        auth.signOut()
    }


    /**
     * Re-authenticates the user with email/password, then deletes the account.
     * Use this when deleteAccount() fails with FirebaseAuthRecentLoginRequiredException.
     *
     * The Auth account is always deleted if reauthentication succeeds, even
     * if Firestore cleanup was incomplete — we don't want to leave the user
     * stuck unable to delete their account because of a transient Firestore
     * error. AccountDeletionResult tells the caller whether to show a
     * "some of your data may not have been fully removed" notice.
     */
    suspend fun reauthenticateWithEmailAndDelete(email: String, password: String): Result<AccountDeletionResult> {
        return try {
            val user       = auth.currentUser ?: return Result.failure(Exception("No user"))
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            val failedSteps = deleteUserFirestoreData(user.uid)
            user.delete().await()
            Result.success(
                if (failedSteps.isEmpty()) AccountDeletionResult.Success
                else AccountDeletionResult.PartialFailure(failedSteps)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Re-authenticates with a fresh Google ID token, then deletes the account.
     */
    suspend fun reauthenticateWithGoogleAndDelete(idToken: String): Result<AccountDeletionResult> {
        return try {
            val user       = auth.currentUser ?: return Result.failure(Exception("No user"))
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            user.reauthenticate(credential).await()
            val failedSteps = deleteUserFirestoreData(user.uid)
            user.delete().await()
            Result.success(
                if (failedSteps.isEmpty()) AccountDeletionResult.Success
                else AccountDeletionResult.PartialFailure(failedSteps)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Re-authenticates with a fresh Facebook token, then deletes the account.
     */
    suspend fun reauthenticateWithFacebookAndDelete(token: String): Result<AccountDeletionResult> {
        return try {
            val user       = auth.currentUser ?: return Result.failure(Exception("No user"))
            val credential = FacebookAuthProvider.getCredential(token)
            user.reauthenticate(credential).await()
            val failedSteps = deleteUserFirestoreData(user.uid)
            user.delete().await()
            Result.success(
                if (failedSteps.isEmpty()) AccountDeletionResult.Success
                else AccountDeletionResult.PartialFailure(failedSteps)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Outcome of deleting a user's Firestore data as part of account deletion.
 * The Auth account is deleted either way (see reauthenticateWith*AndDelete);
 * this only reports whether the linked Firestore data was fully removed,
 * so the UI can show a "some of your data may remain" notice if not.
 */
sealed class AccountDeletionResult {
    object Success : AccountDeletionResult()
    data class PartialFailure(val failedSteps: List<String>) : AccountDeletionResult()
}
