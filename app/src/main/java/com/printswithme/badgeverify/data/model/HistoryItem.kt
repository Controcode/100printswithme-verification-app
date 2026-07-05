package com.printswithme.badgeverify.data.model

data class HistoryItem(
    val verificationId: String,
    val verifiedAt: String,  // ISO timestamp
    val result: VerificationResult
)
