package com.example.muul.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import com.example.muul.data.model.User

class LocalUserRepositoryImpl(context: Context) : UserRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("muul_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_EMAIL = "current_email"
        private const val KEY_USER_PREFIX = "user_"
    }

    override suspend fun register(email: String, password: String): Boolean {
        if (prefs.contains(KEY_USER_PREFIX + email)) return false
        val userJson = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("total_steps", 0)
            put("steps", JSONObject())
            put("profile_photo_uri", JSONObject.NULL)
        }
        prefs.edit().putString(KEY_USER_PREFIX + email, userJson.toString()).apply()
        prefs.edit().putString(KEY_CURRENT_EMAIL, email).apply()
        return true
    }

    override suspend fun login(email: String, password: String): Boolean {
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return false
        val obj = JSONObject(raw)
        val stored = obj.optString("password")
        if (stored == password) {
            prefs.edit().putString(KEY_CURRENT_EMAIL, email).apply()
            return true
        }
        return false
    }

    override fun logout() {
        prefs.edit().remove(KEY_CURRENT_EMAIL).apply()
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

        val newTotalSteps = user.totalSteps + steps
        val newRouteSteps = user.stepsByRoute[routeId].orEmptySteps() + steps

        Log.d(
            "MUUL_STEPS",
            "Guardando $steps pasos para ruta $routeId. Total nuevo: $newTotalSteps"
        )

        saveUserWithSteps(
            email = user.email,
            totalSteps = newTotalSteps,
            stepsByRoute = user.stepsByRoute + (routeId to newRouteSteps)
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
        totalSteps: Int,
        stepsByRoute: Map<String, Int>
    ) {
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return
        val obj = JSONObject(raw)
        val stepsObj = JSONObject()

        stepsByRoute.forEach { (routeId, routeSteps) ->
            stepsObj.put(routeId, routeSteps)
        }

        obj.put("total_steps", totalSteps)
        obj.put("steps", stepsObj)

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
        val email = obj.optString("email")
        val password = obj.optString("password")
        val totalSteps = obj.optInt("total_steps", 0)
        val profilePhotoUri = if (obj.isNull("profile_photo_uri")) {
            null
        } else {
            obj.optString("profile_photo_uri").takeIf { it.isNotBlank() }
        }
        
        val stepsObj = obj.optJSONObject("steps")
        val stepsMap = mutableMapOf<String, Int>()
        if (stepsObj != null) {
            val keys = stepsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                stepsMap[k] = stepsObj.optInt(k, 0)
            }
        }
        
        return User(
            email = email,
            password = password,
            totalSteps = totalSteps,
            stepsByRoute = stepsMap,
            profilePhotoUri = profilePhotoUri
        )
    }
}

private fun Int?.orEmptySteps(): Int = this ?: 0
