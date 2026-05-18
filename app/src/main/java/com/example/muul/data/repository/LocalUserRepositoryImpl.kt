package com.example.muul.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.muul.data.model.User
import org.json.JSONObject

class LocalUserRepositoryImpl(context: Context) : UserRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("muul_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_EMAIL = "current_email"
        private const val KEY_USER_PREFIX = "user_"
    }

    override suspend fun register(user: User): Boolean {
        if (prefs.contains(KEY_USER_PREFIX + user.email)) return false

        val userJson = JSONObject().apply {
            put("id", "local_${user.email}")
            put("nombre", user.nombre)
            put("apellido", user.apellido)
            put("username", user.username)
            put("email", user.email)
            put("password", user.password)
            put("total_steps", 0)
            put("num_pasos", 0)
            put("steps", JSONObject())
            put("profile_photo_uri", JSONObject.NULL)
        }

        prefs.edit {
            putString(KEY_USER_PREFIX + user.email, userJson.toString())
            putString(KEY_CURRENT_EMAIL, user.email)
        }

        Log.d("MUUL_AUTH", "Usuario registrado localmente: ${user.email}")

        return true
    }

    override suspend fun login(emailOrUsername: String, password: String): User? {
        val raw = prefs.getString(KEY_USER_PREFIX + emailOrUsername, null) ?: return null
        val obj = JSONObject(raw)
        val storedPassword = obj.optString("password")

        return if (storedPassword == password) {
            prefs.edit {
                putString(KEY_CURRENT_EMAIL, emailOrUsername)
            }
            Log.d("MUUL_AUTH", "Usuario logueado localmente: $emailOrUsername")
            fromJson(raw)
        } else {
            Log.w("MUUL_AUTH", "Login local fallido para: $emailOrUsername")
            null
        }
    }

    override fun logout() {
        prefs.edit {
            remove(KEY_CURRENT_EMAIL)
        }
        Log.d("MUUL_AUTH", "Sesión local cerrada")
    }

    override fun getCurrentUser(): User? {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return null
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return null

        return fromJson(raw)
    }

    override suspend fun addStepsForRoute(routeId: String, steps: Int) {
        if (steps <= 0) {
            Log.w("MUUL_STEPS", "No se guardan pasos porque steps = $steps para ruta $routeId")
            return
        }

        val user = getCurrentUser() ?: run {
            Log.e("MUUL_STEPS", "No hay usuario actual para guardar pasos")
            return
        }

        val currentSteps = user.totalSteps ?: 0
        val newTotalSteps = currentSteps + steps

        Log.d(
            "MUUL_STEPS",
            "Agregando $steps pasos localmente. Ruta: $routeId. Total anterior: $currentSteps, total nuevo: $newTotalSteps"
        )

        saveUserWithSteps(
            email = user.email,
            totalSteps = newTotalSteps
        )
    }

    override suspend fun updateProfilePhotoUri(uri: String?) {
        val user = getCurrentUser() ?: return
        val raw = prefs.getString(KEY_USER_PREFIX + user.email, null) ?: return
        val obj = JSONObject(raw)

        if (uri.isNullOrBlank()) {
            obj.put("profile_photo_uri", JSONObject.NULL)
        } else {
            obj.put("profile_photo_uri", uri)
        }

        prefs.edit {
            putString(KEY_USER_PREFIX + user.email, obj.toString())
        }
        Log.d("MUUL_USER", "Foto de perfil actualizada localmente para ${user.email}")
    }

    override suspend fun uploadProfilePhoto(bytes: ByteArray, fileName: String): String? {
        // Implementación mock para el repositorio local: simulamos una subida local
        Log.d("MUUL_USER", "Simulando subida local de foto: $fileName")
        return null
    }

    private fun saveUserWithSteps(
        email: String,
        totalSteps: Int
    ) {
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return
        val obj = JSONObject(raw)

        obj.put("total_steps", totalSteps)
        obj.put("num_pasos", totalSteps)

        prefs.edit {
            putString(KEY_USER_PREFIX + email, obj.toString())
        }
    }

    private fun fromJson(raw: String): User {
        val obj = JSONObject(raw)

        val stepsObj = obj.optJSONObject("steps")
        val stepsMap = mutableMapOf<String, Int>()

        if (stepsObj != null) {
            val keys = stepsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                stepsMap[key] = stepsObj.optInt(key, 0)
            }
        }

        val profilePhotoUri = if (obj.isNull("profile_photo_uri")) {
            null
        } else {
            obj.optString("profile_photo_uri").takeIf { it.isNotBlank() }
        }

        val totalStepsVal = obj.optInt("total_steps", obj.optInt("num_pasos", 0))

        return User(
            id = obj.optString("id").takeIf { it.isNotBlank() },
            nombre = obj.optString("nombre"),
            apellido = obj.optString("apellido"),
            username = obj.optString("username"),
            email = obj.optString("email"),
            password = obj.optString("password"),
            numPasos = obj.optInt("num_pasos", totalStepsVal),
            totalSteps = totalStepsVal,
            stepsByRoute = stepsMap,
            profilePhotoUri = profilePhotoUri
        )
    }
}
