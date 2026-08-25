package com.example.castlegame.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegisterScreen(
    onSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var privacyAccepted by remember { mutableStateOf(false) }

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

    val canRegister = authState !is AuthResultState.Loading && privacyAccepted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Register", style = MaterialTheme.typography.headlineMedium)

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
            supportingText = {
                Text("Must be at least 6 characters")
            }
        )

        Spacer(Modifier.height(16.dp))

        // ── Privacy Policy consent (required for Google Play compliance) ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked  = privacyAccepted,
                onCheckedChange = { privacyAccepted = it },
                enabled  = authState !is AuthResultState.Loading
            )
            val annotatedText = buildAnnotatedString {
                append("I have read and accept the ")
                pushStringAnnotation(tag = "PRIVACY_POLICY", annotation = "privacy_policy")
                withStyle(
                    style = SpanStyle(
                        color           = MaterialTheme.colorScheme.primary,
                        fontWeight      = FontWeight.SemiBold,
                        textDecoration  = TextDecoration.Underline
                    )
                ) {
                    append("Privacy Policy")
                }
                pop()
            }
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onPrivacyPolicyClick()
                    }
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.register(email, password, privacyAccepted) },
            modifier = Modifier.fillMaxWidth(),
            enabled = canRegister
        ) {
            if (authState is AuthResultState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Register")
            }
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.align(Alignment.End),
            enabled = authState !is AuthResultState.Loading
        ) {
            Text("Already have an account? Login")
        }


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
