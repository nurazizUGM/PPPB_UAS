package com.example.ppapb_uas.network

import com.example.ppapb_uas.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UserApi {
    @GET("user")
    suspend fun getUsers(): Response<List<User>>

    @GET("user/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<User>

    @POST("user")
    suspend fun createUser(@Body user: User): Response<MessageRes>

    @POST("user/{id}")
    suspend fun updateUser(@Path("id")id: String, @Body user: User): Response<MessageRes>
}