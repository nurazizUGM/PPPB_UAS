package com.example.ppapb_uas.network

import com.example.ppapb_uas.models.Product
import com.example.ppapb_uas.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {
    @GET("posts")
    suspend fun getProducts(@Query("options") query: String = "{\"sort\": {\"created_at\": -1}}"): Response<List<Product>>

    @GET("posts/{id}")
    suspend fun getProductById(@Path("id") id: String): Response<Product>

    @POST("posts")
    suspend fun createProduct(@Body product: Product): Response<MessageRes>

    @POST("posts/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body product: Product): Response<MessageRes>

    @DELETE("posts/{id}")
    suspend fun deleteProduct(@Path("id") id: String): Response<MessageRes>
}