package com.odysee.app.core.designsystem.claims

data class OdyseeClaimMenuTarget(
    val claimId: String,
    val name: String,
    val title: String,
    val permanentUrl: String,
    val channelClaimId: String?,
    val channelName: String?,
)

data class OdyseeClaimMenuActions(
    val onPlayBackground: () -> Unit = {},
    val onPlayPip: () -> Unit = {},
    val onSaveWatchLater: () -> Unit = {},
    val onSaveFavorite: () -> Unit = {},
    val onAddToPlaylist: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onCopyLink: () -> Unit = {},
    val onGoToChannel: (() -> Unit)? = null,
    val onBlockChannel: (() -> Unit)? = null,
    val onRemoveFromPlaylist: (() -> Unit)? = null,
    val onRepost: (() -> Unit)? = null,
    val onReport: () -> Unit = {},
)
