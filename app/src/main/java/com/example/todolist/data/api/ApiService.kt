package com.example.todolist.data.api

import com.example.todolist.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("tasks")
    suspend fun getTasks(@Query("query") query: String? = null): Response<List<Task>>

    @POST("tasks")
    suspend fun createTask(@Body task: TaskRequest): Response<Task>

    @PUT("tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body task: TaskRequest): Response<Task>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Unit>

    @PATCH("tasks/{id}/done")
    suspend fun markDone(@Path("id") id: Int): Response<Task>

    @POST("tasks/{id}/relations")
    suspend fun addRelation(@Path("id") id: Int, @Body body: RelationRequest): Response<Unit>

    @DELETE("tasks/{id}/relations/{relatedId}")
    suspend fun removeRelation(@Path("id") id: Int, @Path("relatedId") relatedId: Int): Response<Unit>
}
