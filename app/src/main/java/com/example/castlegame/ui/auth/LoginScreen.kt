package com.example.castlegame.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.example.castlegame.R
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()

    val callbackManager = remember { CallbackManager.Factory.create() }


    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Reset auth state when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetAuthState()
        }
    }

    // Handle success state
    LaunchedEffect(authState) {
        if (authState is AuthResultState.Success) {
            onSuccess()
            viewModel.resetAuthState()
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
}

