package com.example.muul.data.repository

import com.example.muul.data.model.Ruta
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseRouteRepositoryImpl(
    private val client: SupabaseClient,
    private val userRepository: UserRepository
) : RouteRepository {

    override suspend fun getSavedRoutes(): List<Ruta> = withContext(Dispatchers.IO) {
        val user = userRepository.getCurrentUser() ?: return@withContext emptyList()
        try {
            client.postgrest["rutas"].select {
                filter {
                    Ruta::userId eq user.id
                }
            }.decodeList<Ruta>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun saveRoute(route: Ruta): Ruta = withContext(Dispatchers.IO) {
        val user = userRepository.getCurrentUser() ?: return@withContext route
        try {
            // Manejo de ID para diferenciar local (timestamp) de Supabase (UUID)
            val currentId = route.id
            val isLocalId = currentId != null && !currentId.contains(".") && currentId.toLongOrNull() != null
            
            val routeToSave = route.copy(
                userId = user.id,
                id = if (isLocalId) null else currentId
            )
            
            client.postgrest["rutas"].upsert(routeToSave) {
                select()
            }.decodeSingle<Ruta>()
        } catch (e: Exception) {
            e.printStackTrace()
            route
        }
    }

    override suspend fun deleteRoute(routeId: String) {
        withContext(Dispatchers.IO) {
            try {
                client.postgrest["rutas"].delete {
                    filter {
                        Ruta::id eq routeId
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
