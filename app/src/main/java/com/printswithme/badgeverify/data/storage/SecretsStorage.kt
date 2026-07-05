package com.printswithme.badgeverify.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.printswithme.badgeverify.data.model.SecretKey
import com.printswithme.badgeverify.data.model.SecretsState

private const val PREFS_NAME = "badge_verify_secrets"
private const val KEY_SECRETS = "secrets_v2"
private const val KEY_PENDING_SCAN = "pending_scan_v1"

class SecretsStorage(context: Context) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Fallback to regular prefs if encryption fails (rare, degraded mode)
            context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }
    private val gson = Gson()

    fun getState(): SecretsState {
        val raw = prefs.getString(KEY_SECRETS, null) ?: return SecretsState()
        return try {
            val type = object : TypeToken<SecretsState>() {}.type
            gson.fromJson(raw, type) ?: SecretsState()
        } catch (_: Exception) {
            SecretsState()
        }
    }

    fun addKey(label: String, value: String): SecretsState {
        val trimmedLabel = label.trim().ifEmpty { "Untitled" }
        val trimmedValue = value.trim()
        require(trimmedValue.isNotEmpty()) { "Secret value cannot be empty" }
        val state = getState()
        val key = SecretKey(
            id = generateId(),
            label = trimmedLabel,
            value = trimmedValue,
            addedAt = java.time.Instant.now().toString()
        )
        val next = SecretsState(
            keys = listOf(key) + state.keys,
            activeKeyId = state.activeKeyId ?: key.id  // auto-activate if first
        )
        save(next)
        return next
    }

    fun deleteKey(id: String): SecretsState {
        val state = getState()
        val next = SecretsState(
            keys = state.keys.filter { it.id != id },
            activeKeyId = if (state.activeKeyId == id) null else state.activeKeyId
        )
        save(next)
        return next
    }

    fun setActiveKey(id: String?): SecretsState {
        val state = getState()
        val validId = if (id != null && state.keys.any { it.id == id }) id else null
        val next = state.copy(activeKeyId = validId)
        save(next)
        return next
    }

    fun getActiveSecretValue(): String {
        val state = getState()
        if (state.activeKeyId == null) return ""
        return state.keys.find { it.id == state.activeKeyId }?.value ?: ""
    }

    // Pending scanned secret — used to transfer QR data from scanner → secrets screen
    fun setPendingScannedSecret(value: String) {
        prefs.edit().putString(KEY_PENDING_SCAN, value).apply()
    }

    fun consumePendingScannedSecret(): String? {
        val value = prefs.getString(KEY_PENDING_SCAN, null) ?: return null
        prefs.edit().remove(KEY_PENDING_SCAN).apply()
        return value
    }

    private fun save(state: SecretsState) {
        prefs.edit().putString(KEY_SECRETS, gson.toJson(state)).apply()
    }

    private fun generateId(): String =
        "${System.currentTimeMillis()}-${(1_000_000..9_999_999).random()}"
}
