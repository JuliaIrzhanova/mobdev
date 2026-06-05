package io.github.mobdev.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.mobdev.network.models.ImageData
import io.github.mobdev.network.models.Message
import io.github.mobdev.network.models.MessageData
import io.github.mobdev.network.models.TextData
import kotlinx.coroutines.flow.first

private val Context.chatDataStore: DataStore<Preferences> by preferencesDataStore(name = "chat_cache")

class ChatLocalStore(context: Context) {

    private val dataStore = context.chatDataStore
    private val gson = Gson()

    // Каналы

    suspend fun saveChannels(channels: List<String>) {
        dataStore.edit { it[KEY_CHANNELS] = gson.toJson(channels) }
    }

    suspend fun getChannels(): List<String> {
        val json = dataStore.data.first()[KEY_CHANNELS] ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    // Сообщения

    suspend fun saveMessages(channel: String, messages: List<Message>) {
        val dtos = messages.map { it.toDto() }
        dataStore.edit { it[channelKey(channel)] = gson.toJson(dtos) }
    }

    suspend fun getMessages(channel: String): List<Message> {
        val json = dataStore.data.first()[channelKey(channel)] ?: return emptyList()
        val type = object : TypeToken<List<CachedMessageDto>>() {}.type
        val dtos: List<CachedMessageDto> = gson.fromJson(json, type)
        return dtos.map { it.toMessage() }
    }

    // Pending сообщения

    suspend fun getPendingMessages(): List<PendingOutgoingMessage> {
        val json = dataStore.data.first()[KEY_PENDING] ?: return emptyList()
        return parsePendingList(json)
    }

    suspend fun getPendingMessagesForChannel(channel: String): List<PendingOutgoingMessage> =
        getPendingMessages().filter { it.channel == channel }

    suspend fun addPendingMessage(message: PendingOutgoingMessage) {
        dataStore.edit { prefs ->
            val current = parsePendingList(prefs[KEY_PENDING])
            prefs[KEY_PENDING] = gson.toJson(current + message)
        }
    }

    suspend fun removePendingMessage(localId: String) {
        dataStore.edit { prefs ->
            val current = parsePendingList(prefs[KEY_PENDING])
            prefs[KEY_PENDING] = gson.toJson(current.filterNot { it.localId == localId })
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    //Вспомогательные

    private fun parsePendingList(json: String?): List<PendingOutgoingMessage> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<PendingOutgoingMessage>>() {}.type
        return runCatching { gson.fromJson<List<PendingOutgoingMessage>>(json, type) }
            .getOrDefault(emptyList())
    }

    private fun channelKey(channel: String) =
        stringPreferencesKey("messages_${channel.replace('@', '_')}")

    private companion object {
        val KEY_CHANNELS = stringPreferencesKey("channels")
        val KEY_PENDING = stringPreferencesKey("pending_outgoing")
    }
}

//  Конвертеры

private fun Message.toDto() = CachedMessageDto(
    id = id,
    from = from,
    type = if (data.image != null) "image" else "text",
    text = data.text?.text,
    imageLink = data.image?.link,
    time = time
)

private fun CachedMessageDto.toMessage() = Message(
    id = id,
    from = from,
    data = if (type == "image") {
        MessageData(image = ImageData(link = imageLink))
    } else {
        MessageData(text = TextData(text = text ?: ""))
    },
    time = time
)

fun PendingOutgoingMessage.toMessage() = Message(
    id = localId,
    from = from,
    data = MessageData(text = TextData(text = text)),
    time = (createdAt / 1000).toString()
)
