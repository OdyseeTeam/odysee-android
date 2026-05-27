package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelStatsDto(
    @SerialName("ChannelSubs") val channelSubs: Long = 0,
    @SerialName("ChannelSubChange") val channelSubChange: Long = 0,
    @SerialName("AllContentViews") val allContentViews: Long = 0,
    @SerialName("AllContentViewChange") val allContentViewChange: Long = 0,
    @SerialName("VideoURITopNew") val videoUriTopNew: String? = null,
    @SerialName("VideoViewsTopNew") val videoViewsTopNew: Long = 0,
    @SerialName("VideoViewChangeTopNew") val videoViewChangeTopNew: Long = 0,
    @SerialName("VideoURITopCommentNew") val videoUriTopCommentNew: String? = null,
    @SerialName("VideoCommentTopCommentNew") val videoCommentTopCommentNew: Long = 0,
    @SerialName("VideoCommentChangeTopCommentNew") val videoCommentChangeTopCommentNew: Long = 0,
    @SerialName("VideoURITopAllTime") val videoUriTopAllTime: String? = null,
    @SerialName("VideoViewsTopAllTime") val videoViewsTopAllTime: Long = 0,
    @SerialName("VideoViewChangeTopAllTime") val videoViewChangeTopAllTime: Long = 0,
)
