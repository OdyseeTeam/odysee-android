package com.odysee.app.core.model

data class Channel(
    val claimId: String,
    val name: String,
    val title: String?,
    val description: String?,
    val thumbnailUrl: String?,
    val coverUrl: String?,
    val permanentUrl: String,
    val canonicalUrl: String?,
    val tags: List<String>,
    val languages: List<String>,
    val email: String?,
    val websiteUrl: String?,
    val stakedAmount: Double,
    val claimsInChannel: Long?,
    val creationTimestamp: Long?,
    val modifiedAt: Long?,
    val featuredUris: List<String> = emptyList(),
)
