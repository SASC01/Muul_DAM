package com.example.muul.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import com.example.muul.data.model.User

class UserRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("muul_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_EMAIL = "current_email"
        private const val KEY_USER_PREFIX = "user_" // user_<email> -> JSON
    }

    fun register(email: String, password: String): Boolean {
        if (prefs.contains(KEY_USER_PREFIX + email)) return false
        val userJson = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("total_steps", 0)
            put("steps", JSONObject())
        }
        prefs.edit().putString(KEY_USER_PREFIX + email, userJson.toString()).apply()
        prefs.edit().putString(KEY_CURRENT_EMAIL, email).apply()
        Log.d("MUUL_AUTH", "Usuario registrado: $email")
        return true
    }

    fun login(email: String, password: String): Boolean {
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return false
        val obj = JSONObject(raw)
        val stored = obj.optString("password")
        if (stored == password) {
            prefs.edit().putString(KEY_CURRENT_EMAIL, email).apply()
            Log.d("MUUL_AUTH", "Usuario logueado: $email")
            return true
        }
        return false
    }

    fun logout() {
        prefs.edit().remove(KEY_CURRENT_EMAIL).apply()
    }

    fun getCurrentUser(): User? {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return null
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return null
        return fromJson(raw)
    }

    fun addStepsForRoute(routeId: String, steps: Int) {
        val user = getCurrentUser() ?: return
        val newTotalSteps = user.totalSteps + steps
        Log.d("MUUL_STEPS", "Agregando $steps pasos para ruta $routeId. Total anterior: ${user.totalSteps}, Total nuevo: $newTotalSteps")
        saveUserWithSteps(user.email, newTotalSteps)
    }

    private fun saveUserWithSteps(email: String, totalSteps: Int) {
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return
        val obj = JSONObject(raw)
        obj.put("total_steps", totalSteps)
        prefs.edit().putString(KEY_USER_PREFIX + email, obj.toString()).apply()
        Log.d("MUUL_STEPS", "Guardados $totalSteps pasos para $email")
    }

    private fun fromJson(raw: String): User {
        val obj = JSONObject(raw)
        val email = obj.optString("email")
        val password = obj.optString("password")
        val totalSteps = obj.optInt("total_steps", 0)
        
        // Mantener compatibilidad con formato anterior
        val stepsObj = obj.optJSONObject("steps")
        val stepsMap = mutableMapOf<String, Int>()
        if (stepsObj != null) {
            val keys = stepsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                stepsMap[k] = stepsObj.optInt(k, 0)
            }
        }
        
        Log.d("MUUL_USER", "Cargando usuario: $email, totalSteps: $totalSteps")
        return User(email = email, password = password, totalSteps = totalSteps, stepsByRoute = stepsMap)
    }
}
