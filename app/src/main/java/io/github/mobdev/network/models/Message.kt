package io.github.mobdev.network.models

import com.google.gson.annotations.SerializedName

data class Message(
    @SerializedName("id")   val id: String,
    @SerializedName("from") val from: String,
    @SerializedName("to")   val to: String? = null,
    @SerializedName("data") val data: MessageData,
    @SerializedName("time") val time: String? = null
)

data class MessageData(
    @SerializedName("Text")  val text: TextData? = null,
    @SerializedName("Image") val image: ImageData? = null
)

data class TextData(
    @SerializedName("text") val text: String
)

data class ImageData(
    @SerializedName("link") val link: String? = null
)

data class LoginRequest(
    @SerializedName("name") val name: String,
    @SerializedName("pwd")  val pwd: String
)

data class SendMessage(
    @SerializedName("from") val from: String,
    @SerializedName("to")   val to: String,
    @SerializedName("data") val data: MessageData
)