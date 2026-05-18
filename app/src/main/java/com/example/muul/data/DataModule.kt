package com.example.muul.data

import android.content.Context
import com.example.muul.data.remote.SupabaseProvider
import com.example.muul.data.repository.RouteRepository
import com.example.muul.data.repository.SupabaseRouteRepositoryImpl
import com.example.muul.data.repository.SupabaseUserRepositoryImpl
import com.example.muul.data.repository.UserRepository

object DataModule {
    private var userRepository: UserRepository? = null
    private var routeRepository: RouteRepository? = null

    fun getUserRepository(context: Context): UserRepository {
        return userRepository ?: synchronized(this) {
            userRepository ?: SupabaseUserRepositoryImpl(SupabaseProvider.client).also { userRepository = it }
        }
    }

    fun getRouteRepository(context: Context): RouteRepository {
        return routeRepository ?: synchronized(this) {
            val userRepo = getUserRepository(context)
            routeRepository ?: SupabaseRouteRepositoryImpl(SupabaseProvider.client, userRepo).also { routeRepository = it }
        }
    }
}
