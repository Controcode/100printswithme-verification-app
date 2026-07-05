package com.printswithme.badgeverify.data.model

import com.google.gson.annotations.SerializedName

data class VerificationResult(
    @SerializedName("id") val id: String = "",
    @SerializedName("public_data") val publicData: Map<String, Any?>? = null,
    @SerializedName("is_private") val isPrivate: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("company_name") val companyName: String = "",
    @SerializedName("company_reason") val companyReason: String = ""
)
