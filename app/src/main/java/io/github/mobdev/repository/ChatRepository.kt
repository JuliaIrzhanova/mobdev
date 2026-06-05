package io.github.mobdev.repository

import android.content.Context
import io.github.mobdev.local.ChatLocalStore
import io.github.mobdev.local.PendingOutgoingMessage
import io.github.mobdev.local.toMessage
import io.github.mobdev.network.ApiClient
import io.github.mobdev.network.NetworkMonitor
import io.github.mobdev.network.models.Message
import io.github.mobdev.network.models.MessageData
import io.github.mobdev.network.models.SendMessage
import io.github.mobdev.network.models.TextData
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class ChatRepository private constructor(
    private val local: ChatLocalStore,
    val networkMonitor: NetworkMonitor,
) {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    // ── Каналы ────────────────────────────────────────────────────────────────

    suspend fun fetchChannels(): Result<List<String>> {
        if (!isOnline.value) {
            val cached = local.getChannels()
            return if (cached.isNotEmpty()) Result.success(cached)
            else Result.failure(Exception("offline"))
        }
        return runCatching {
            val response = ApiClient.service.getChannels()
            if (response.isSuccessful) {
                val channels = response.body() ?: emptyList()
                local.saveChannels(channels)
                channels
            } else throw Exception("HTTP ${response.code()}")
        }.recoverCatching { error ->
            val cached = local.getChannels()
            if (cached.isNotEmpty()) cached else throw error
        }
    }

    // ── Сообщения ─────────────────────────────────────────────────────────────

    suspend fun loadCachedMessages(channel: String): List<Message> {
        val cached = local.getMessages(channel)
        val pending = local.getPendingMessagesForChannel(channel).map { it.toMessage() }
        val ids = cached.map { it.id }.toSet()
        return cached + pending.filter { it.id !in ids }
    }

    suspend fun fetchMessages(channel: String, token: String): Result<List<Message>> {
        if (!isOnline.value) {
            val merged = loadCachedMessages(channel)
            return if (merged.isNotEmpty()) Result.success(merged)
            else Result.failure(Exception("offline"))
        }
        return runCatching {
            val response = ApiClient.service.getChannelMessages(
                channel = channel, limit = 20, lastKnownId = "0",
                reverse = false, token = token
            )
            when {
                response.isSuccessful -> {
                    val messages = response.body() ?: emptyList()
                    local.saveMessages(channel, messages)
                    messages
                }
                response.code() == 401 -> throw Exception("401")
                else -> throw Exception("HTTP ${response.code()}")
            }
        }.recoverCatching { error ->
            if (error.message == "401") throw error
            val merged = loadCachedMessages(channel)
            if (merged.isNotEmpty()) merged else throw error
        }.map { loadCachedMessages(channel) }
    }

    // ── Отправка ──────────────────────────────────────────────────────────────

    suspend fun sendMessage(token: String, from: String, channel: String, text: String): Result<Boolean> {
        // Boolean: true = отправлено, false = добавлено в очередь
        if (!isOnline.value) {
            local.addPendingMessage(
                PendingOutgoingMessage(
                    localId = "pending-${UUID.randomUUID()}",
                    channel = channel,
                    from = from,
                    text = text,
                    createdAt = System.currentTimeMillis()
                )
            )
            return Result.success(false)
        }
        return runCatching {
            val msg = SendMessage(from = from, to = channel, data = MessageData(text = TextData(text)))
            val response = ApiClient.service.sendMessage(token, msg)
            if (!response.isSuccessful) throw Exception(response.code().toString())
            true
        }
    }

    // ── Очередь pending ───────────────────────────────────────────────────────

    suspend fun flushPendingMessages(token: String, from: String) {
        if (!isOnline.value) return
        val pending = local.getPendingMessages()
        for (msg in pending) {
            runCatching {
                ApiClient.service.sendMessage(
                    token,
                    SendMessage(from = from, to = msg.channel, data = MessageData(text = TextData(msg.text)))
                )
            }.onSuccess {
                local.removePendingMessage(msg.localId)
            }
        }
    }

    // ── Выход ─────────────────────────────────────────────────────────────────

    suspend fun logout(token: String) {
        if (isOnline.value) runCatching { ApiClient.service.logout(token) }
        local.clearAll()
    }

    companion object {
        @Volatile
        private var INSTANCE: ChatRepository? = null

        fun getInstance(context: Context): ChatRepository =
            INSTANCE ?: synchronized(this) {
                val monitor = NetworkMonitor(context.applicationContext)
                monitor.start()
                ChatRepository(
                    local = ChatLocalStore(context.applicationContext),
                    networkMonitor = monitor
                ).also { INSTANCE = it }
            }
    }
}
