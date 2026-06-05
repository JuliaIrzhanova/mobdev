package io.github.mobdev.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import io.github.mobdev.ChatApplication
import io.github.mobdev.prefs.PrefsManager
import kotlinx.coroutines.launch

class ChannelsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PrefsManager(application)
    private val repository = (application as ChatApplication).repository

    private val _channels = MutableLiveData<List<String>>(emptyList())
    val channels: LiveData<List<String>> = _channels

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    val isOnline: LiveData<Boolean> = repository.isOnline.asLiveData()

    val username: String get() = prefs.login ?: ""

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            repository.fetchChannels()
                .onSuccess { _channels.value = it }
                .onFailure { if (it.message != "offline") _error.value = it.message }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.logout(prefs.token ?: "")
            prefs.clear()
            onDone()
        }
    }
}
