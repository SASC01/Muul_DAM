package com.example.muul.data.local

import android.content.Context
import android.content.SharedPreferences
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
            put("steps", JSONObject())
        }
        prefs.edit().putString(KEY_USER_PREFIX + email, userJson.toString()).apply()
        prefs.edit().putString(KEY_CURRENT_EMAIL, email).apply()
        return true
    }

    fun login(email: String, password: String): Boolean {
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return false
        val obj = JSONObject(raw)
        val stored = obj.optString("password")
        if (stored == password) {
            prefs.edit().putString(KEY_CURRENT_EMAIL, email).apply()
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
        val mutable = user.stepsByRoute.toMutableMap()
        mutable[routeId] = (mutable[routeId] ?: 0) + steps
        saveSteps(user.email, mutable)
    }

    private fun saveSteps(email: String, stepsMap: Map<String, Int>) {
        val raw = prefs.getString(KEY_USER_PREFIX + email, null) ?: return
        val obj = JSONObject(raw)
        val stepsObj = JSONObject()
        stepsMap.forEach { (k, v) -> stepsObj.put(k, v) }
        obj.put("steps", stepsObj)
        prefs.edit().putString(KEY_USER_PREFIX + email, obj.toString()).apply()
    }

    private fun fromJson(raw: String): User {
        val obj = JSONObject(raw)
        val email = obj.optString("email")
        val password = obj.optString("password")
        val stepsObj = obj.optJSONObject("steps")
        val stepsMap = mutableMapOf<String, Int>()
        if (stepsObj != null) {
            val keys = stepsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                stepsMap[k] = stepsObj.optInt(k, 0)
            }
        }
        return User(email = email, password = password, stepsByRoute = stepsMap)
    }
}
