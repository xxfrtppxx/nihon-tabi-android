package com.nihontabi.android.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihontabi.android.core.network.AuthApi
import com.nihontabi.android.core.network.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val authApi: AuthApi,
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = tokenStore.tokens
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, tokenStore.tokens.value != null)

    fun logout() {
        viewModelScope.launch {
            runCatching { authApi.logout() }
            tokenStore.clear()
        }
    }
}
