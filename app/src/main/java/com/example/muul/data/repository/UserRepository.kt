package com.example.muul.data.repository

import com.example.muul.data.model.User

interface UserRepository {
    suspend fun register(email: String, password: String): Boolean
    suspend fun login(email: String, password: String): Boolean
    fun logout()
    fun getCurrentUser(): User?
    suspend fun addStepsForRoute(routeId: String, steps: Int)
    suspend fun updateProfilePhotoUri(uri: String?)
}
