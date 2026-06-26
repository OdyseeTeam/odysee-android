package com.odysee.app.core.data.reactions

import com.odysee.app.core.network.LbryioApi
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
        val others = data.othersReactions[claimId]
        val mine = data.myReactions[claimId]
        val myReaction = when {
            (mine?.like ?: 0) > 0 -> MyReaction.LIKE
            (mine?.dislike ?: 0) > 0 -> MyReaction.DISLIKE
            else -> MyReaction.NONE
        }
        return Reactions(
            likes = others?.like ?: 0L,
            dislikes = others?.dislike ?: 0L,
            myReaction = myReaction,
        )
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
