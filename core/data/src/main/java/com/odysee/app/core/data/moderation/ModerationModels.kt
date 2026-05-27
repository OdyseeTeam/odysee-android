package com.odysee.app.core.data.moderation

data class BlockedCommenter(
    val claimId: String,
    val name: String,
    val blockedAt: String? = null,
    val bannedUntil: String? = null,
    val scope: BlockScope = BlockScope.Channel,
)

enum class BlockScope { Channel, Global, Delegated }

data class CommentModerator(
    val claimId: String,
    val name: String,
)
