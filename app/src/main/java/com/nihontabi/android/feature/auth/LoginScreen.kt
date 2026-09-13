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
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.loginState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Nihon Tabi 日本旅", style = MaterialTheme.typography.labelLarge)
        Text(
            "Log in",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onLoginEmailChange,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onLoginPasswordChange,
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
            onClick = { viewModel.login(onLoggedIn) },
            enabled = !state.submitting,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Log in")
            }
        }

        TextButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Text("Don't have an account? Sign up")
        }
    }
}
