package io.usewise.android.persistence

import android.content.Context
import android.content.SharedPreferences

class UsewiseStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("usewise_prefs", Context.MODE_PRIVATE)

    fun getString(key: String): String? = prefs.getString(key, null)
    fun setString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getBool(key: String): Boolean = prefs.getBoolean(key, false)
    fun setBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun remove(key: String) = prefs.edit().remove(key).apply()
}
