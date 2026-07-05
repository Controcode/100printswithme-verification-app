package com.printswithme.badgeverify.data.model

data class SecretKey(
    val id: String,
    val label: String,
    val value: String,
    val addedAt: String  // ISO timestamp
)

data class SecretsState(
    val keys: List<SecretKey> = emptyList(),
    val activeKeyId: String? = null
)
