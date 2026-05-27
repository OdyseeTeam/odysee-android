package com.odysee.app.core.data.discussion

data class DiscussionSettings(
    val minTipAmountComment: Double = 0.0,
    val minTipAmountSuperChat: Double = 0.0,
    val slowModeMinGap: Int = 0,
    val commentsMembersOnly: Boolean = false,
    val livestreamChatMembersOnly: Boolean = false,
)
