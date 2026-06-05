package io.github.mobdev

import android.app.Application
import io.github.mobdev.repository.ChatRepository

class ChatApplication : Application() {
    val repository: ChatRepository by lazy {
        ChatRepository.getInstance(this)
    }
}
