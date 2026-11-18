package com.alitaalab.medicalfollow.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Keeps the parent's Claude API key in app-private storage on this phone.
 * The file is excluded from cloud backups (see res/xml/backup_rules.xml and data_extraction_rules.xml).
 */
class ApiKeyStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val _key = MutableStateFlow(prefs.getString(KEY, "") ?: "")
    val key: StateFlow<String> = _key.asStateFlow()

    fun save(value: String) {
        val v = value.trim()
        prefs.edit().putString(KEY, v).apply()
        _key.value = v
    }

    fun clear() = save("")

    companion object {
        const val FILE = "secrets"
        private const val KEY = "claude_api_key"
    }
}
