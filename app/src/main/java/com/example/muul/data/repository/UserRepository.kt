package com.example.muul.data.repository

import com.example.muul.data.model.User

interface UserRepository {
    suspend fun register(user: User): Boolean
    suspend fun login(emailOrUsername: String, password: String): User?
    fun logout()
    fun getCurrentUser(): User?
    suspend fun addStepsForRoute(routeId: String, steps: Int)
    suspend fun updateProfilePhotoUri(uri: String?)
}
