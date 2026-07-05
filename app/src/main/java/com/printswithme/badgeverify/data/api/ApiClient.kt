package com.printswithme.badgeverify.data.api

import com.google.gson.JsonObject
import com.printswithme.badgeverify.data.model.VerificationResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLDecoder

private const val BASE_URL = "https://api.100printswith.me/"

object ApiClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: BadgeApi = retrofit.create(BadgeApi::class.java)
}

/**
 * Executes a badge verification call and normalises the response into a [VerifyResult].
 * Mirrors the logic in the RN `verifyBadge()` function in api.ts.
 */
suspend fun BadgeApi.verifyBadgeSafe(
    rawId: String,
    secret: String? = null
): VerifyResult {
    val trimmed = rawId.trim()
    if (trimmed.isEmpty()) {
        return VerifyResult.Failure(0, "Verification ID is required")
    }

    // Extract id if a full URL was scanned
    var id = trimmed
    var effectiveSecret = secret
    try {
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            val uri = android.net.Uri.parse(trimmed)
            val segments = uri.pathSegments
            if (segments.isNotEmpty()) id = segments.last()
            val qs = uri.getQueryParameter("secret")
            if (qs != null && effectiveSecret == null) effectiveSecret = qs
        }
    } catch (_: Exception) { /* ignore */ }

    return try {
        val response = verifyBadge(id, effectiveSecret?.ifEmpty { null })
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                VerifyResult.Success(body)
            } else {
                VerifyResult.Failure(response.code(), "Empty response from server")
            }
        } else {
            val code = response.code()
            var message = "Verification failed ($code)"
            var needsSecret = false
            try {
                val errorBody = response.errorBody()?.string()
                if (!errorBody.isNullOrBlank()) {
                    val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                    message = when {
                        json.has("detail") -> json.get("detail").asString
                        json.has("message") -> json.get("message").asString
                        else -> message
                    }
                }
            } catch (_: Exception) { /* ignore */ }
            if (code == 401 || code == 403) {
                needsSecret = true
                if (message.startsWith("Verification failed")) {
                    message = "This badge is private. A secret key is required."
                }
            } else if (code == 404) {
                message = "Badge not found. Please check the verification ID."
            }
            VerifyResult.Failure(code, message, needsSecret)
        }
    } catch (e: Exception) {
        VerifyResult.Failure(0, "Network error: ${e.message ?: "Check your connection."}")
    }
}
