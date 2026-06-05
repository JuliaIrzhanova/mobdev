package io.github.mobdev.repository

import android.content.Context
import io.github.mobdev.db.AppDatabase
import io.github.mobdev.db.ChannelEntity
import io.github.mobdev.db.MessageEntity
import io.github.mobdev.network.ApiClient
import io.github.mobdev.network.NetworkUtils
import io.github.mobdev.network.models.Message
import io.github.mobdev.network.models.MessageData
import io.github.mobdev.network.models.SendMessage
import io.github.mobdev.network.models.TextData

class ChatRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val channelDao = db.channelDao()
    private val messageDao = db.messageDao()

    fun isOnline(): Boolean = NetworkUtils.isOnline(context)

    // ── Каналы ────────────────────────────────────────────────────────────────

    suspend fun getChannels(): List<String> {
        if (isOnline()) {
            try {
                val response = ApiClient.service.getChannels()
                if (response.isSuccessful) {
                    val channels = response.body() ?: emptyList()
                    channelDao.clearAll()
                    channelDao.insertAll(channels.map { ChannelEntity(it) })
                    return channels
                }
            } catch (_: Exception) {}
        }
        // офлайн или ошибка — отдаём кэш
        return channelDao.getAll()
    }

    // ── Сообщения ─────────────────────────────────────────────────────────────

    suspend fun getMessages(channel: String, token: String): Pair<List<Message>, Boolean> {
        // Сначала отдаём кэш, потом пробуем сеть
        val cached = messageDao.getByChannel(channel).map { it.toMessage() }

        if (!isOnline()) {
            return Pair(cached, false)
        }

        return try {
            val response = ApiClient.service.getChannelMessages(
                channel = channel,
                limit = 20,
                lastKnownId = "0",
                reverse = false,
                token = token
            )
            if (response.isSuccessful) {
                val messages = response.body() ?: emptyList()
                messageDao.insertAll(messages.map { it.toEntity(channel) })
                // Возвращаем объединённый список без дублей (по id)
                val networkIds = messages.map { it.id }.toSet()
                val merged = cached.filter { it.id !in networkIds } + messages
                Pair(merged.sortedBy { it.time }, true)
            } else {
                Pair(cached, response.code() != 401)
            }
        } catch (_: Exception) {
            Pair(cached, false)
        }
    }

    // ── Отправка ──────────────────────────────────────────────────────────────

    suspend fun sendMessage(token: String, from: String, to: String, text: String): Result<Unit> {
        if (!isOnline()) return Result.failure(Exception("no_network"))
        return try {
            val msg = SendMessage(
                from = from,
                to = to,
                data = MessageData(text = TextData(text))
            )
            val response = ApiClient.service.sendMessage(token, msg)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.code().toString()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── Конвертеры ────────────────────────────────────────────────────────────────

private fun Message.toEntity(channel: String): MessageEntity {
    val isImage = data.image != null
    return MessageEntity(
        id = id,
        fromUser = from,
        channel = channel,
        dataType = if (isImage) "image" else "text",
        textContent = data.text?.text,
        imageLink = data.image?.link,
        time = time
    )
}

private fun MessageEntity.toMessage(): Message {
    val data = if (dataType == "image") {
        io.github.mobdev.network.models.MessageData(
            image = io.github.mobdev.network.models.ImageData(link = imageLink)
        )
    } else {
        io.github.mobdev.network.models.MessageData(
            text = io.github.mobdev.network.models.TextData(text = textContent ?: "")
        )
    }
    return Message(id = id, from = fromUser, to = channel, data = data, time = time)
}
