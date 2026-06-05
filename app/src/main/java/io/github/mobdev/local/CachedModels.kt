package io.github.mobdev.local

data class CachedMessageDto(
    val id: String,
    val from: String,
    val type: String,       // "text" or "image"
    val text: String?,
    val imageLink: String?,
    val time: String?
)

data class PendingOutgoingMessage(
    val localId: String,
    val channel: String,
    val from: String,
    val text: String,
    val createdAt: Long
)
