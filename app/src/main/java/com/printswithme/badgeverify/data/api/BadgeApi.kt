package com.printswithme.badgeverify.data.api

import com.printswithme.badgeverify.data.model.VerificationResult
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BadgeApi {
    @GET("api/v1/private/verification/{id}")
    suspend fun verifyBadge(
        @Path("id") id: String,
        @Query("secret") secret: String? = null
    ): Response<VerificationResult>
}

sealed class VerifyResult {
    data class Success(val data: VerificationResult) : VerifyResult()
    data class Failure(
        val status: Int,
        val message: String,
        val needsSecret: Boolean = false
    ) : VerifyResult()
}
