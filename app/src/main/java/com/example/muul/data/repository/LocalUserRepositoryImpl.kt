package com.example.muul.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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

        val savedUser = prefs.edit()
            .putString(KEY_USER_PREFIX + user.email, userJson.toString())
            .commit()

        val savedSession = prefs.edit()
            .putString(KEY_CURRENT_EMAIL, user.email)
            .commit()

        Log.d(
            "MUUL_AUTH",
            "Usuario registrado localmente: ${user.email}, savedUser=$savedUser, savedSession=$savedSession"
        )

        return savedUser && savedSession
    }

    override suspend fun login(emailOrUsername: String, password: String): User? {
        val raw = prefs.getString(KEY_USER_PREFIX + emailOrUsername, null) ?: return null
        val obj = JSONObject(raw)
        val storedPassword = obj.optString("password")

        return if (storedPassword == password) {
            prefs.edit()
                .putString(KEY_CURRENT_EMAIL, emailOrUsername)
                .apply()

            Log.d("MUUL_AUTH", "Usuario logueado localmente: $emailOrUsername")

            fromJson(raw)
        } else {
            Log.w("MUUL_AUTH", "Login local fallido para: $emailOrUsername")
            null
        }
    }

    override fun logout() {
        prefs.edit()
            .remove(KEY_CURRENT_EMAIL)
            .apply()

        Log.d("MUUL_AUTH", "Sesión local cerrada")
    }

    override fun getCurrentUser(): User? {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return null
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return null

        return fromJson(raw)
    }

    override suspend fun addStepsForRoute(routeId: String, steps: Int) {
        if (steps <= 0) {
            Log.w(
                "MUUL_STEPS",
                "No se guardan pasos porque steps = $steps para ruta $routeId"
            )
            return
        }

        val user = getCurrentUser()

        if (user == null) {
            Log.e("MUUL_STEPS", "No hay usuario actual para guardar pasos")
            return
        }

        val newTotalSteps = user.totalSteps + steps

        Log.d(
            "MUUL_STEPS",
            "Agregando $steps pasos. Ruta: $routeId. Total anterior: ${user.totalSteps}, total nuevo: $newTotalSteps"
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

        val saved = prefs.edit()
            .putString(KEY_USER_PREFIX + user.email, obj.toString())
            .commit()

        if (saved) {
            Log.d("MUUL_USER", "Foto de perfil actualizada para ${user.email}")
        } else {
            Log.e("MUUL_USER", "No se pudo actualizar foto de perfil para ${user.email}")
        }
    }

    private fun saveUserWithSteps(
        email: String,
        totalSteps: Int
    ) {
        val raw = prefs.getString(KEY_USER_PREFIX + email, null)

        if (raw == null) {
            Log.e("MUUL_STEPS", "No se encontró el usuario local $email")
            return
        }

        val obj = JSONObject(raw)

        obj.put("total_steps", totalSteps)

        // Mantener sincronizado este campo por compatibilidad con la versión remota/Supabase.
        obj.put("num_pasos", totalSteps)

        val saved = prefs.edit()
            .putString(KEY_USER_PREFIX + email, obj.toString())
            .commit()

        if (saved) {
            Log.d("MUUL_STEPS", "Pasos guardados para $email: $totalSteps")
        } else {
            Log.e("MUUL_STEPS", "No se pudieron persistir pasos para $email")
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

        val totalSteps = obj.optInt(
            "total_steps",
            obj.optInt("num_pasos", 0)
        )

        return User(
            id = obj.optString("id").takeIf { it.isNotBlank() },
            nombre = obj.optString("nombre"),
            apellido = obj.optString("apellido"),
            username = obj.optString("username"),
            email = obj.optString("email"),
            password = obj.optString("password"),
            numPasos = obj.optInt("num_pasos", totalSteps),
            totalSteps = totalSteps,
            stepsByRoute = stepsMap,
            profilePhotoUri = profilePhotoUri
        )
    }
}