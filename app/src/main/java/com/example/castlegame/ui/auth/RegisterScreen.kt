package com.example.castlegame.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegisterScreen(
    onSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()

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

        Button(
            onClick = { viewModel.register(email, password) },
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


/*
package com.example.castlegame.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.castlegame.ui.auth.AuthResultState
import com.example.castlegame.ui.auth.AuthViewModel

@Composable
fun RegisterScreen(
    onSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.register(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        when (authState) {
            is AuthResultState.Error -> {
                Text(
                    text = (authState as AuthResultState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            AuthResultState.Success -> onSuccess()
            else -> {}
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Already have an account? Login")
        }

    }
}
*/
