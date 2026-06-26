package com.odysee.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape of `POST reaction/list`. Both nested maps are keyed by
 * claim ID — typically the single claim we asked about, but the API
 * supports batched fetches so the wire shape is `claimId → counts`.
 *
 * `others_reactions` holds aggregate community counts; `my_reactions`
 * holds the active user's own contributions (zero unless they've liked
 * or disliked that claim).
 */
@Serializable
data class ReactionListResult(
    @SerialName("others_reactions") val othersReactions: Map<String, ReactionCounts> = emptyMap(),
    @SerialName("my_reactions") val myReactions: Map<String, ReactionCounts> = emptyMap(),
)

@Serializable
data class ReactionCounts(
    val like: Long = 0,
    val dislike: Long = 0,
)
