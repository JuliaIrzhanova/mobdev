package io.github.mobdev.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val fromUser: String,
    val channel: String,
    val dataType: String,       // "text" или "image"
    val textContent: String?,
    val imageLink: String?,
    val time: String?
)
