package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteStatusDto(
    @SerialName("invites_remaining") val invitesRemaining: Int? = null,
    val invitees: List<InviteeDto>? = null,
)

@Serializable
data class InviteeDto(
    val email: String? = null,
    @SerialName("invite_accepted") val inviteAccepted: Boolean? = null,
    @SerialName("invite_reward_claimed") val inviteRewardClaimed: Boolean? = null,
    @SerialName("invite_reward_claimable") val inviteRewardClaimable: Boolean? = null,
    @SerialName("invited_at") val invitedAt: String? = null,
)
