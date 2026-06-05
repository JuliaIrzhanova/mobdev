package io.github.mobdev.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.mobdev.prefs.PrefsManager
import io.github.mobdev.repository.ChatRepository
import kotlinx.coroutines.launch

class ChannelsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PrefsManager(application)
    private val repository = ChatRepository(application)

    private val _channels = MutableLiveData<List<String>>(emptyList())
    val channels: LiveData<List<String>> = _channels

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _isOnline = MutableLiveData(true)
    val isOnline: LiveData<Boolean> = _isOnline

    val username: String get() = prefs.login ?: ""

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _isOnline.value = repository.isOnline()
            try {
                val channels = repository.getChannels()
                _channels.value = channels
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            val token = prefs.token ?: ""
            try {
                io.github.mobdev.network.ApiClient.service.logout(token)
            } catch (_: Exception) {}
            prefs.clear()
            onDone()
        }
    }
}
