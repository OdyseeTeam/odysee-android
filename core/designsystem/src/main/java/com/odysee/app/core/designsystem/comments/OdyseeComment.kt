package com.odysee.app.core.designsystem.comments

enum class OdyseeMembershipTier { None, Premium, PremiumPlus }

enum class OdyseeReaction { None, Like, Dislike }

data class OdyseeComment(
    val id: String,
    val parentId: String? = null,
    val authorDisplayName: String,
    val authorHandle: String,
    val authorChannelId: String?,
    val authorAvatarUrl: String?,
    val authorInitial: Char,
    val ageLabel: String,
    val body: String,
    val isPinned: Boolean = false,
    val pinnedByName: String? = null,
    val isEdited: Boolean = false,
    val isCreator: Boolean = false,
    val isMine: Boolean = false,
    val isModerator: Boolean = false,
    val isGlobalMod: Boolean = false,
    val membership: OdyseeMembershipTier = OdyseeMembershipTier.None,
    val creatorMembership: String? = null,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val myReaction: OdyseeReaction = OdyseeReaction.None,
    val supportAmount: Double = 0.0,
    val creatorLiked: Boolean = false,
    val creatorAvatarUrl: String? = null,
    val replyCount: Int = 0,
)

data class OdyseeCommentActions(
    val onLike: () -> Unit = {},
    val onDislike: () -> Unit = {},
    val onReply: ((String) -> Unit)? = null,
    val onVisitChannel: () -> Unit = {},
    val onCopyText: () -> Unit = {},
    val onCopyLink: () -> Unit = {},
    val onPinToggle: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onBlock: () -> Unit = {},
    val onAddModerator: () -> Unit = {},
    val onRemoveModerator: () -> Unit = {},
    val onReport: () -> Unit = {},
    val onHashtagClick: (String) -> Unit = {},
)
