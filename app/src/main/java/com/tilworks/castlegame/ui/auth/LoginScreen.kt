package com.tilworks.castlegame.ui.auth

import com.tilworks.castlegame.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
    profileRepository: UserProfileRepository = remember { UserProfileRepository() }
) {
    val authState by viewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()

    val callbackManager = remember { CallbackManager.Factory.create() }


    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // First-time social sign-in consent gate
    var showConsentDialog by remember { mutableStateOf(false) }
    var consentChecked by remember { mutableStateOf(false) }
    var socialLoginPending by remember { mutableStateOf(false) }

    // Reset auth state when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetAuthState()
        }
    }


    // Handle success state.
    // Email/password logins go straight through — they either went through RegisterScreen
    // (which already required consent) or are returning users.
    // Social logins (Google/Facebook) check for a profile first — only first-time users
    // (no profile yet) see the consent dialog.
    LaunchedEffect(authState) {
        if (authState is AuthResultState.Success) {
            if (!socialLoginPending) {
                // Email/password login — skip profile check, go straight through
                onSuccess()
                viewModel.resetAuthState()
            } else {
                // Social login — check if profile already exists
                val existingProfile = profileRepository.getProfile().getOrNull()
                socialLoginPending = false
                if (existingProfile == null) {
                    // Brand-new social account → show consent dialog before creating profile
                    showConsentDialog = true
                } else {
                    // Returning social user — already consented previously
                    onSuccess()
                    viewModel.resetAuthState()
                }
            }
        }
    }

    val context = LocalContext.current
    val clientId = stringResource(R.string.default_web_client_id)

    val googleSignInClient = remember(context, clientId) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()

        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                viewModel.loginWithGoogle(idToken)
            }

        } catch (e: ApiException) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthResultState.Loading,
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthResultState.Loading,
            singleLine = true,
            // Add password visibility toggle if desired
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = authState !is AuthResultState.Loading
        ) {
            if (authState is AuthResultState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login")
            }
        }

        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.align(Alignment.End),
            enabled = authState !is AuthResultState.Loading
        ) {
            Text("No account? Register")
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                launcher.launch(googleSignInClient.signInIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue with Google")
        }


        Spacer(Modifier.height(12.dp))

        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                LoginButton(context).apply {
                    // Így a helyes: közvetlenül a listát adjuk át
                    setReadPermissions(listOf("email", "public_profile"))

                    registerCallback(callbackManager,
                        object : FacebookCallback<LoginResult> {
                            override fun onSuccess(result: LoginResult) {
                                val token = result.accessToken.token
                                viewModel.loginWithFacebook(token)
                            }

                            override fun onCancel() {
                                // Opcionális: Logolhatod, ha a felhasználó meggondolta magát
                            }

                            override fun onError(error: FacebookException) {
                                error.printStackTrace()
                            }
                        }
                    )
                }
            }
        )

        // Error message display
        if (authState is AuthResultState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = (authState as AuthResultState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    // ── First-time social sign-in consent dialog ─────────────────────────────
    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { /* must accept or cancel sign-in, no outside-tap dismiss */ },
            title = { Text("Welcome!") },
            text = {
                Column {
                    Text(
                        "Before you start playing, please review and accept our Privacy Policy.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = consentChecked,
                            onCheckedChange = { consentChecked = it }
                        )
                        val annotatedText = buildAnnotatedString {
                            append("I have read and accept the ")
                            withStyle(
                                style = SpanStyle(
                                    color          = MaterialTheme.colorScheme.primary,
                                    fontWeight     = FontWeight.SemiBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append("Privacy Policy")
                            }
                        }
                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onPrivacyPolicyClick() }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uid   = FirebaseAuth.getInstance().currentUser?.uid ?: return@Button
                        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                        scope.launch {
                            val newProfile = UserProfile(
                                id                       = uid,
                                email                    = email,
                                privacyPolicyAccepted   = true,
                                privacyPolicyAcceptedAt = Timestamp.now()
                            )
                            profileRepository.createProfile(newProfile)
                            showConsentDialog   = false
                            socialLoginPending = false
                            onSuccess()
                            viewModel.resetAuthState()
                        }
                    },
                    enabled = consentChecked
                ) {
                    Text("Accept & Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // User declined → sign them out and stay on Login
                        FirebaseAuth.getInstance().signOut()
                        showConsentDialog   = false
                        socialLoginPending = false
                        consentChecked       = false
                        viewModel.resetAuthState()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
