package com.smartexambox.security.network

import android.content.Context
import android.content.SharedPreferences

class PairingPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "http://192.168.1.39:8080") ?: "http://192.168.1.39:8080"
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim()).apply()

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var isPaired: Boolean
        get() = prefs.getBoolean(KEY_IS_PAIRED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_PAIRED, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    fun clearPairing() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .putBoolean(KEY_IS_PAIRED, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "smart_exam_pairing_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_IS_PAIRED = "is_paired"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
    }
}
