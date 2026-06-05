package io.github.mobdev.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import io.github.mobdev.ChatApplication
import io.github.mobdev.network.models.Message
import io.github.mobdev.prefs.PrefsManager
import kotlinx.coroutines.launch

class MessagesViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val prefs = PrefsManager(application)
    private val repository = (application as ChatApplication).repository

    var channel: String
        get() = savedStateHandle["channel"] ?: "1@channel"
        set(value) { savedStateHandle["channel"] = value }

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _sending = MutableLiveData(false)
    val sending: LiveData<Boolean> = _sending

    // true = отправлено, false = добавлено в очередь офлайн
    private val _sendResult = MutableLiveData<Boolean?>(null)
    val sendResult: LiveData<Boolean?> = _sendResult

    val isOnline: LiveData<Boolean> = repository.isOnline.asLiveData()

    val username: String get() = prefs.login ?: ""

    fun loadMessages() {
        viewModelScope.launch {
            val token = prefs.token ?: return@launch

            // Сразу показываем кэш — как в референсе
            val cached = repository.loadCachedMessages(channel)
            if (cached.isNotEmpty()) {
                _messages.value = cached
            }

            // Затем пробуем обновить из сети
            repository.fetchMessages(channel, token)
                .onSuccess { _messages.value = it }
                .onFailure {
                    when (it.message) {
                        "401" -> _error.value = "401"
                        "offline" -> { /* кэш уже показан выше */ }
                        else -> if (cached.isEmpty()) _error.value = it.message
                    }
                }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _sending.value = true
            val token = prefs.token ?: run { _sending.value = false; return@launch }
            repository.sendMessage(token, username, channel, text)
                .onSuccess { sent ->
                    _sendResult.value = sent
                    _sendResult.value = null
                    loadMessages()
                }
                .onFailure {
                    if (it.message == "401") _error.value = "401"
                    else _error.value = it.message
                }
            _sending.value = false
        }
    }

    fun flushPendingMessages() {
        viewModelScope.launch {
            val token = prefs.token ?: return@launch
            repository.flushPendingMessages(token, username)
            loadMessages()
        }
    }
}
