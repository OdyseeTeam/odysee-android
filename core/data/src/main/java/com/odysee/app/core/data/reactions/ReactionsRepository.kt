package com.odysee.app.core.data.reactions

import com.odysee.app.core.network.LbryioApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class Reactions(
    val likes: Long,
    val dislikes: Long,
    val myReaction: MyReaction,
)

enum class MyReaction { LIKE, DISLIKE, NONE }

interface ReactionsRepository {
    suspend fun fetch(claimId: String): Reactions?
    suspend fun like(claimId: String, remove: Boolean): Boolean
    suspend fun dislike(claimId: String, remove: Boolean): Boolean
}

private const val LIKE = "like"
private const val DISLIKE = "dislike"

@Singleton
class ReactionsRepositoryImpl @Inject constructor(
    private val lbryioApi: LbryioApi,
) : ReactionsRepository {

    override suspend fun fetch(claimId: String): Reactions? {
        val env = runCatching { lbryioApi.reactionList(claimId) }.getOrNull() ?: return null
        val data = env.data ?: return null
        val others = (data["others_reactions"] as? JsonObject)?.get(claimId) as? JsonObject
        val mine = (data["my_reactions"] as? JsonObject)?.get(claimId) as? JsonObject
        val likes = (others?.get(LIKE)?.jsonPrimitive?.longOrNull) ?: 0L
        val dislikes = (others?.get(DISLIKE)?.jsonPrimitive?.longOrNull) ?: 0L
        val myLike = (mine?.get(LIKE)?.jsonPrimitive?.longOrNull) ?: 0L
        val myDislike = (mine?.get(DISLIKE)?.jsonPrimitive?.longOrNull) ?: 0L
        val myReaction = when {
            myLike > 0 -> MyReaction.LIKE
            myDislike > 0 -> MyReaction.DISLIKE
            else -> MyReaction.NONE
        }
        return Reactions(likes = likes, dislikes = dislikes, myReaction = myReaction)
    }

    override suspend fun like(claimId: String, remove: Boolean): Boolean = runCatching {
        lbryioApi.reactionReact(
            claimIds = claimId,
            type = LIKE,
            clearTypes = DISLIKE,
            remove = if (remove) true else null,
        )
    }.isSuccess

    override suspend fun dislike(claimId: String, remove: Boolean): Boolean = runCatching {
        lbryioApi.reactionReact(
            claimIds = claimId,
            type = DISLIKE,
            clearTypes = LIKE,
            remove = if (remove) true else null,
        )
    }.isSuccess
}
