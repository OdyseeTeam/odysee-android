package com.odysee.app.core.data.analytics

data class ChannelStats(
    val subscribers: Long,
    val subscriberChange: Long,
    val totalViews: Long,
    val viewChange: Long,
    val topNewVideoUri: String?,
    val topNewVideoViews: Long,
    val topNewVideoViewChange: Long,
    val topCommentedVideoUri: String?,
    val topCommentedVideoComments: Long,
    val topCommentedVideoCommentChange: Long,
    val topAllTimeVideoUri: String?,
    val topAllTimeVideoViews: Long,
    val topAllTimeVideoViewChange: Long,
)
