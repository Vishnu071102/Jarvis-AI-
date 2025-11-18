package com.yourdomain.jarvis.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecurePrefs {
    private const val PREF_FILE = "jarvis_secure_prefs"
    private const val KEY_API = "openai_api_key"

    private fun prefs(ctx: Context) = EncryptedSharedPreferences.create(
        PREF_FILE,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        ctx,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(ctx: Context, apiKey: String) {
        prefs(ctx).edit().putString(KEY_API, apiKey).apply()
    }

    fun getApiKey(ctx: Context): String? = prefs(ctx).getString(KEY_API, null)

    fun clearApiKey(ctx: Context) { prefs(ctx).edit().remove(KEY_API).apply() }
}
