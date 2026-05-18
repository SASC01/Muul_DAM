package com.example.muul.data.repository

import com.example.muul.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
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

            client.postgrest["usuarios"].update({
                User::totalSteps setTo newTotal
                User::numPasos setTo steps
                User::stepsByRoute setTo newStepsByRoute
            }) {
                filter { User::id eq user.id }
            }
            
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

    override suspend fun uploadProfilePhoto(bytes: ByteArray, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            // Cambiado de "avatars" a "profile_photo" para coincidir con tu bucket en Supabase
            val bucket = client.storage["profile_photo"]
            bucket.upload(path = fileName, data = bytes) {
                upsert = true
            }
            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
