package io.github.mobdev.network

import io.github.mobdev.network.models.LoginRequest
import io.github.mobdev.network.models.Message
import io.github.mobdev.network.models.SendMessage
import retrofit2.Response
import retrofit2.http.*
import okhttp3.ResponseBody

interface ApiService {

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ResponseBody>

    @POST("logout")
    suspend fun logout(
        @Header("X-Auth-Token") token: String
    ): Response<Unit>

    @GET("channels")
    suspend fun getChannels(): Response<List<String>>

    @GET("channel/{channel}")
    suspend fun getChannelMessages(
        @Path("channel") channel: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String = "0",
        @Query("reverse") reverse: Boolean = false,
        @Header("X-Auth-Token") token: String
    ): Response<List<Message>>

    @POST("messages")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Body message: SendMessage
    ): Response<String>
}