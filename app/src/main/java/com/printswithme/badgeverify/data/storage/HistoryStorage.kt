package com.printswithme.badgeverify.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.printswithme.badgeverify.data.model.HistoryItem

private const val PREFS_NAME = "badge_verify_history"
private const val KEY_HISTORY = "history_v1"
private const val MAX_HISTORY = 200

class HistoryStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<HistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(raw, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(item: HistoryItem) {
        val current = getAll()
        val filtered = current.filter { it.verificationId != item.verificationId }
        val next = (listOf(item) + filtered).take(MAX_HISTORY)
        save(next)
    }

    fun delete(verificationId: String): List<HistoryItem> {
        val next = getAll().filter { it.verificationId != verificationId }
        save(next)
        return next
    }

    fun deleteOlderThan(cutoffMs: Long): List<HistoryItem> {
        val cutoff = System.currentTimeMillis() - cutoffMs
        val next = getAll().filter { item ->
            try {
                val t = java.time.Instant.parse(item.verifiedAt).toEpochMilli()
                t >= cutoff
            } catch (_: Exception) {
                true // Keep items with unparseable timestamps
            }
        }
        save(next)
        return next
    }

    fun clear(): List<HistoryItem> {
        prefs.edit().remove(KEY_HISTORY).apply()
        return emptyList()
    }

    private fun save(items: List<HistoryItem>) {
        prefs.edit().putString(KEY_HISTORY, gson.toJson(items)).apply()
    }
}
