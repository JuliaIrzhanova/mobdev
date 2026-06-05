package io.github.mobdev.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.mobdev.network.models.Message
import io.github.mobdev.prefs.PrefsManager
import io.github.mobdev.repository.ChatRepository
import kotlinx.coroutines.launch

class MessagesViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val prefs = PrefsManager(application)
    private val repository = ChatRepository(application)

    var channel: String
        get() = savedStateHandle["channel"] ?: "1@channel"
        set(value) { savedStateHandle["channel"] = value }

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _sending = MutableLiveData(false)
    val sending: LiveData<Boolean> = _sending

    private val _isOnline = MutableLiveData(true)
    val isOnline: LiveData<Boolean> = _isOnline

    val username: String get() = prefs.login ?: ""

    fun loadMessages() {
        viewModelScope.launch {
            val token = prefs.token ?: return@launch
            _isOnline.value = repository.isOnline()
            try {
                val (messages, online) = repository.getMessages(channel, token)
                _isOnline.value = online
                _messages.value = messages
            } catch (e: Exception) {
                if (repository.isOnline()) _error.value = e.message
                // офлайн — просто показываем что есть в кэше, ошибку не показываем
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (!repository.isOnline()) {
            _error.value = "no_network"
            return
        }
        viewModelScope.launch {
            _sending.value = true
            val token = prefs.token ?: run {
                _sending.value = false
                return@launch
            }
            val result = repository.sendMessage(token, username, channel, text)
            result.fold(
                onSuccess = { loadMessages() },
                onFailure = { e ->
                    if (e.message == "401") _error.value = "401"
                    else _error.value = e.message
                }
            )
            _sending.value = false
        }
    }
}
