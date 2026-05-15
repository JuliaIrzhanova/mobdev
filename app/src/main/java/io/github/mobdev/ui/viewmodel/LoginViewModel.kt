package io.github.mobdev.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.mobdev.network.models.LoginRequest
import io.github.mobdev.prefs.PrefsManager
import kotlinx.coroutines.launch
import io.github.mobdev.network.ApiClient

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PrefsManager(application)

    private val _state = MutableLiveData<LoginState>(LoginState.Idle)
    val state: LiveData<LoginState> = _state

    fun hasSavedCredentials(): Boolean =
        prefs.login != null && prefs.token != null

    fun login(name: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val response = ApiClient.service.login(LoginRequest(name, password))
                when {
                    response.isSuccessful -> {
                        val token = response.body()?.string() ?: ""
                        prefs.login = name
                        prefs.password = password
                        prefs.token = token.trim()
                        _state.value = LoginState.Success
                    }
                    response.code() == 401 -> {
                        _state.value = LoginState.Error("wrong_credentials")
                    }
                    else -> {
                        _state.value = LoginState.Error("Ошибка сервера: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error("Нет соединения: ${e.message}")
            }
        }
    }
}