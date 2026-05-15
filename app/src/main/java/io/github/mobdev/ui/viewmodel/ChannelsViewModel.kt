package io.github.mobdev.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.mobdev.network.ApiClient
import io.github.mobdev.prefs.PrefsManager
import kotlinx.coroutines.launch

class ChannelsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PrefsManager(application)

    private val _channels = MutableLiveData<List<String>>(emptyList())
    val channels: LiveData<List<String>> = _channels

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    val username: String get() = prefs.login ?: ""

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            try {
                val response = ApiClient.service.getChannels()
                when {
                    response.isSuccessful -> {
                        _channels.value = response.body() ?: emptyList()
                    }
                    response.code() == 401 -> {
                        _error.value = "401"
                    }
                    else -> {
                        _error.value = "Ошибка: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            val token = prefs.token ?: ""
            try {
                ApiClient.service.logout(token)
            } catch (_: Exception) {}
            prefs.clear()
            onDone()
        }
    }
}