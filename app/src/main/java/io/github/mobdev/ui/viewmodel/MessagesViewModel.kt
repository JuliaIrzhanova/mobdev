package io.github.mobdev.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.mobdev.network.ApiClient
import io.github.mobdev.network.models.Message
import io.github.mobdev.network.models.MessageData
import io.github.mobdev.network.models.SendMessage
import io.github.mobdev.network.models.TextData
import io.github.mobdev.prefs.PrefsManager
import kotlinx.coroutines.launch

class MessagesViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val prefs = PrefsManager(application)

    var channel: String
        get() = savedStateHandle["channel"] ?: "1@channel"
        set(value) { savedStateHandle["channel"] = value }

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _sending = MutableLiveData(false)
    val sending: LiveData<Boolean> = _sending

    val username: String get() = prefs.login ?: ""

    fun loadMessages() {
        viewModelScope.launch {
            try {
                val token = prefs.token ?: return@launch
                val response = ApiClient.service.getChannelMessages(
                    channel = channel,
                    limit = 20,
                    lastKnownId = "0",
                    reverse = false,
                    token = token
                )
                when {
                    response.isSuccessful -> {
                        _messages.value = response.body() ?: emptyList()
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

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _sending.value = true
            try {
                val token = prefs.token ?: return@launch
                val msg = SendMessage(
                    from = username,
                    to = channel,
                    data = MessageData(text = TextData(text))
                )
                val response = ApiClient.service.sendMessage(token, msg)
                when {
                    response.isSuccessful -> {
                        loadMessages()
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
            } finally {
                _sending.value = false
            }
        }
    }
}