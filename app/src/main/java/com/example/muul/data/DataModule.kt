package com.example.muul.data

import android.content.Context
import com.example.muul.data.repository.LocalRouteRepositoryImpl
import com.example.muul.data.repository.LocalUserRepositoryImpl
import com.example.muul.data.repository.RouteRepository
import com.example.muul.data.repository.UserRepository

object DataModule {
    private var userRepository: UserRepository? = null
    private var routeRepository: RouteRepository? = null

    fun getUserRepository(context: Context): UserRepository {
        return userRepository ?: synchronized(this) {
            userRepository ?: LocalUserRepositoryImpl(context).also { userRepository = it }
        }
    }

    fun getRouteRepository(context: Context): RouteRepository {
        return routeRepository ?: synchronized(this) {
            val userRepo = getUserRepository(context)
            routeRepository ?: LocalRouteRepositoryImpl(context, userRepo).also { routeRepository = it }
        }
    }
}
