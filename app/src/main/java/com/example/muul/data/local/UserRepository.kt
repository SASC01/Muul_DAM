package com.example.muul.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.muul.data.model.User
import org.json.JSONObject

class UserRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(
            "muul_user_prefs",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val KEY_CURRENT_EMAIL = "current_email"
        private const val KEY_USER_PREFIX = "user_"
    }

    fun register(email: String, password: String): Boolean {
        val cleanEmail = email.trim()

        if (prefs.contains(KEY_USER_PREFIX + cleanEmail)) {
            Log.w("MUUL_AUTH", "Registro fallido: usuario ya existe $cleanEmail")
            return false
        }

        val userJson = JSONObject().apply {
            put("email", cleanEmail)
            put("password", password)
            put("total_steps", 0)
            put("steps", JSONObject())
        }

        val saved = prefs.edit()
            .putString(KEY_USER_PREFIX + cleanEmail, userJson.toString())
            .putString(KEY_CURRENT_EMAIL, cleanEmail)
            .commit()

        Log.d("MUUL_AUTH", "Usuario registrado: $cleanEmail, saved: $saved")

        return saved
    }

    fun login(email: String, password: String): Boolean {
        val cleanEmail = email.trim()
        val raw = prefs.getString(KEY_USER_PREFIX + cleanEmail, null) ?: return false
        val obj = JSONObject(raw)
        val storedPassword = obj.optString("password")

        return if (storedPassword == password) {
            val saved = prefs.edit()
                .putString(KEY_CURRENT_EMAIL, cleanEmail)
                .commit()

            Log.d("MUUL_AUTH", "Usuario logueado: $cleanEmail, saved: $saved")
            saved
        } else {
            Log.w("MUUL_AUTH", "Login fallido para: $cleanEmail")
            false
        }
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_CURRENT_EMAIL)
            .apply()

        Log.d("MUUL_AUTH", "Sesión cerrada")
    }

    fun getCurrentUser(): User? {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return null
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return null

        return fromJson(raw)
    }

    fun addStepsForRoute(routeId: String, steps: Int) {
        if (steps <= 0) {
            Log.w(
                "MUUL_STEPS",
                "No se guardan pasos porque steps = $steps para ruta $routeId"
            )
            return
        }

        val user = getCurrentUser()

        if (user == null) {
            Log.e("MUUL_STEPS", "No hay usuario actual, no se pueden guardar pasos")
            return
        }

        val newTotalSteps = user.totalSteps + steps

        Log.d(
            "MUUL_STEPS",
            "Agregando $steps pasos. Ruta: $routeId. Total anterior: ${user.totalSteps}, total nuevo: $newTotalSteps"
        )

        saveUserWithSteps(user.email, newTotalSteps)
    }

    private fun saveUserWithSteps(email: String, totalSteps: Int) {
        val raw = prefs.getString(KEY_USER_PREFIX + email, null)

        if (raw == null) {
            Log.e("MUUL_STEPS", "No se encontró JSON del usuario $email")
            return
        }

        val obj = JSONObject(raw)
        obj.put("total_steps", totalSteps)

        val saved = prefs.edit()
            .putString(KEY_USER_PREFIX + email, obj.toString())
            .commit()

        if (saved) {
            Log.d("MUUL_STEPS", "Guardados $totalSteps pasos para $email")
        } else {
            Log.e("MUUL_STEPS", "Error guardando pasos para $email")
        }
    }

    private fun fromJson(raw: String): User {
        val obj = JSONObject(raw)

        val email = obj.optString("email")
        val password = obj.optString("password")
        val totalSteps = obj.optInt("total_steps", 0)

        val stepsObj = obj.optJSONObject("steps")
        val stepsMap = mutableMapOf<String, Int>()

        if (stepsObj != null) {
            val keys = stepsObj.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                stepsMap[key] = stepsObj.optInt(key, 0)
            }
        }

        Log.d(
            "MUUL_USER",
            "Cargando usuario: $email, totalSteps: $totalSteps"
        )

        return User(
            email = email,
            password = password,
            totalSteps = totalSteps,
            stepsByRoute = stepsMap
        )
    }
}