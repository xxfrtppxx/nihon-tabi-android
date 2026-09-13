package com.nihontabi.android.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihontabi.android.ui.theme.extendedColors

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.registerState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Nihon Tabi 日本旅", style = MaterialTheme.typography.labelLarge)
        Text(
            "Create an account",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::onRegisterDisplayNameChange,
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onRegisterEmailChange,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onRegisterPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        if (state.error != null) {
            Text(
                state.error!!,
                color = MaterialTheme.extendedColors.plan,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = { viewModel.register(onRegistered) },
            enabled = !state.submitting,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Sign up")
            }
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Text("Already have an account? Log in")
        }
    }
}
