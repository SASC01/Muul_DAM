package com.example.muul.data.repository

import com.example.muul.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseUserRepositoryImpl(private val client: SupabaseClient) : UserRepository {

    private var currentUser: User? = null

    override suspend fun register(user: User): Boolean = withContext(Dispatchers.IO) {
        try {
            val userToInsert = user.copy(id = null)
            client.postgrest["usuarios"].insert(userToInsert)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun login(emailOrUsername: String, password: String): User? = withContext(Dispatchers.IO) {
        try {
            val user = client.postgrest["usuarios"].select {
                filter {
                    or {
                        User::email eq emailOrUsername
                        User::username eq emailOrUsername
                    }
                    User::password eq password
                }
            }.decodeSingleOrNull<User>()

            currentUser = user
            user
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun logout() {
        currentUser = null
    }

    override fun getCurrentUser(): User? = currentUser

    override suspend fun addStepsForRoute(routeId: String, steps: Int) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        try {
            val newTotal = (user.totalSteps ?: 0) + steps
            val newStepsByRoute = user.stepsByRoute?.toMutableMap() ?: mutableMapOf()
            newStepsByRoute[routeId] = (newStepsByRoute[routeId] ?: 0) + steps

            // Actualizamos total_steps, num_pasos (pasos de esta ruta) y el JSON de rutas
            client.postgrest["usuarios"].update({
                User::totalSteps setTo newTotal
                User::numPasos setTo steps // Guardamos los pasos capturados por el sensor en esta sesión
                User::stepsByRoute setTo newStepsByRoute
            }) {
                filter { User::id eq user.id }
            }
            
            // Sincronizar el usuario local
            currentUser = user.copy(
                totalSteps = newTotal, 
                numPasos = steps, 
                stepsByRoute = newStepsByRoute
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun updateProfilePhotoUri(uri: String?) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        try {
            client.postgrest["usuarios"].update({
                User::profilePhotoUri setTo uri
            }) {
                filter { User::id eq user.id }
            }
            currentUser = user.copy(profilePhotoUri = uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
