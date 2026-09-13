package com.nihontabi.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihontabi.android.core.network.AuthApi
import com.nihontabi.android.core.network.AuthTokens
import com.nihontabi.android.core.network.TokenStore
import com.nihontabi.android.core.network.dto.LoginRequest
import com.nihontabi.android.core.network.dto.RegisterRequest
import com.nihontabi.android.core.network.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
)

data class RegisterFormState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginFormState())
    val loginState: StateFlow<LoginFormState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterFormState())
    val registerState: StateFlow<RegisterFormState> = _registerState.asStateFlow()

    fun onLoginEmailChange(value: String) = _loginState.update { it.copy(email = value, error = null) }
    fun onLoginPasswordChange(value: String) = _loginState.update { it.copy(password = value, error = null) }

    fun onRegisterDisplayNameChange(value: String) = _registerState.update { it.copy(displayName = value, error = null) }
    fun onRegisterEmailChange(value: String) = _registerState.update { it.copy(email = value, error = null) }
    fun onRegisterPasswordChange(value: String) = _registerState.update { it.copy(password = value, error = null) }

    fun login(onSuccess: () -> Unit) {
        val state = _loginState.value
        if (state.submitting) return
        viewModelScope.launch {
            _loginState.update { it.copy(submitting = true, error = null) }
            try {
                val result = authApi.login(LoginRequest(state.email, state.password))
                tokenStore.setTokens(AuthTokens(result.accessToken, result.refreshToken))
                _loginState.update { it.copy(submitting = false) }
                onSuccess()
            } catch (e: Exception) {
                _loginState.update { it.copy(submitting = false, error = e.toUserMessage(json)) }
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        val state = _registerState.value
        if (state.submitting) return
        viewModelScope.launch {
            _registerState.update { it.copy(submitting = true, error = null) }
            try {
                val result = authApi.register(
                    RegisterRequest(
                        email = state.email,
                        password = state.password,
                        displayName = state.displayName,
                    ),
                )
                tokenStore.setTokens(AuthTokens(result.accessToken, result.refreshToken))
                _registerState.update { it.copy(submitting = false) }
                onSuccess()
            } catch (e: Exception) {
                _registerState.update { it.copy(submitting = false, error = e.toUserMessage(json)) }
            }
        }
    }
}
