package com.odysee.app.core.data

import com.odysee.app.core.model.Claim
import com.odysee.app.core.model.Homepage
import com.odysee.app.core.model.HomepageCategory
import com.odysee.app.core.model.Channel
import com.odysee.app.core.model.Comment
import com.odysee.app.core.network.CommentronApi
import com.odysee.app.core.network.LbryioApi
import com.odysee.app.core.network.LighthouseApi
import com.odysee.app.core.network.LivestreamApi
import com.odysee.app.core.network.OdyseeContentApi
import com.odysee.app.core.network.SdkProxyApi
import kotlinx.serialization.json.contentOrNull
import com.odysee.app.core.network.dto.ChannelSignParams
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.odysee.app.core.network.dto.ClaimSearchParams
import com.odysee.app.core.network.dto.CommentAbandonParams
import com.odysee.app.core.network.dto.CommentCreateParams
import com.odysee.app.core.network.dto.CommentEditParams
import com.odysee.app.core.network.dto.CommentListParams
import com.odysee.app.core.network.dto.CommentPinParams
import com.odysee.app.core.network.dto.CommentReactListParams
import com.odysee.app.core.network.dto.CommentReactParams
import com.odysee.app.core.network.dto.GetStreamParams
import com.odysee.app.core.network.dto.SupportCreateParams
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.unwrap
import com.odysee.app.core.network.mapper.toChannel
import com.odysee.app.core.network.mapper.toDomain
import javax.inject.Inject
import javax.inject.Singleton

data class CommentReactionCounts(
    val likes: Int = 0,
    val dislikes: Int = 0,
    val myLike: Boolean = false,
    val myDislike: Boolean = false,
    val creatorLike: Boolean = false,
)

interface ContentRepository {
    suspend fun getHomepage(lang: String = "en"): Homepage
    suspend fun getCategoryFeed(
        category: HomepageCategory,
        page: Int = 1,
        homepageLanguage: String? = null,
    ): List<Claim>
    suspend fun resolveStreamUrl(uri: String): String
    suspend fun getComments(
        claimId: String,
        page: Int = 1,
        pageSize: Int = 50,
        sortBy: Int = 3,
        parentId: String? = null,
    ): List<Comment>

    suspend fun getCommentReactions(
        commentIds: List<String>,
        myChannelId: String? = null,
        myChannelName: String? = null,
    ): Map<String, CommentReactionCounts>

    suspend fun reactToComment(
        commentId: String,
        channelId: String,
        channelName: String,
        type: String,
        remove: Boolean,
    )

    suspend fun pinComment(
        commentId: String,
        channelId: String,
        channelName: String,
        remove: Boolean,
    )

    suspend fun editComment(
        commentId: String,
        text: String,
        channelId: String,
        channelName: String,
    ): Comment

    suspend fun deleteComment(commentId: String, channelId: String, channelName: String)
    suspend fun blockCommenter(
        modChannelId: String,
        modChannelName: String,
        blockedChannelId: String,
        blockedChannelName: String,
        creatorChannelId: String?,
        creatorChannelName: String?,
        offendingCommentId: String?,
        blockAll: Boolean,
        timeoutSec: Long?,
    )
    suspend fun addCommentModerator(
        modChannelId: String,
        modChannelName: String,
        creatorChannelId: String,
        creatorChannelName: String,
    )
    suspend fun removeCommentModerator(
        modChannelId: String,
        modChannelName: String,
        creatorChannelId: String,
        creatorChannelName: String,
    )
    suspend fun listBlockedCommenters(
        creatorChannelId: String,
        creatorChannelName: String,
    ): List<com.odysee.app.core.data.moderation.BlockedCommenter>
    suspend fun listCommentModerators(
        creatorChannelId: String,
        creatorChannelName: String,
    ): List<com.odysee.app.core.data.moderation.CommentModerator>
    suspend fun unblockCommenter(
        modChannelId: String,
        modChannelName: String,
        blockedChannelId: String,
        blockedChannelName: String,
        creatorChannelId: String?,
        creatorChannelName: String?,
    )
    suspend fun getChannel(claimId: String): Channel?
    suspend fun getChannels(claimIds: List<String>): List<Channel>
    suspend fun getClaimsByIds(claimIds: List<String>): List<Claim>
    suspend fun getChannelCollections(channelClaimId: String): List<com.odysee.app.core.network.dto.CollectionClaimDto>
    suspend fun getChannelVideos(channelClaimId: String, page: Int = 1, pageSize: Int = 20): List<Claim>
    suspend fun getFollowingFeed(channelClaimIds: List<String>, page: Int = 1, pageSize: Int = 30): List<Claim>
    suspend fun search(query: String, size: Int = 20, from: Int = 0): List<Claim>
    suspend fun getRelated(channelClaimId: String?, excludeClaimId: String, query: String? = null): List<Claim>
    suspend fun getViewCounts(claimIds: List<String>): Map<String, Long>
    suspend fun getShortsFeed(page: Int = 1, pageSize: Int = 20): List<Claim>
    suspend fun getLivestreams(pageSize: Int = 20): List<Claim>
    suspend fun getLivestreamUrls(): Map<String, String>
    suspend fun getUpcoming(pageSize: Int = 20): List<Claim>
    suspend fun postComment(
        claimId: String,
        channelId: String,
        channelName: String,
        text: String,
        parentId: String? = null,
        supportTxId: String? = null,
    ): Comment

    suspend fun sendTip(claimId: String, amountLbc: Double, channelId: String? = null): String
    suspend fun getFeaturedChannelClaimIds(channelClaimId: String): List<String>
    suspend fun getFeaturedChannelSections(channelClaimId: String): List<com.odysee.app.core.data.featured.FeaturedChannelSection>
    suspend fun updateFeaturedChannelSections(
        channelClaimId: String,
        channelName: String,
        sections: List<com.odysee.app.core.data.featured.FeaturedChannelSection>,
    )
    suspend fun getDiscussionSettings(channelClaimId: String): com.odysee.app.core.data.discussion.DiscussionSettings
    suspend fun updateDiscussionSettings(
        channelClaimId: String,
        channelName: String,
        settings: com.odysee.app.core.data.discussion.DiscussionSettings,
    )
    suspend fun listMemberships(channelClaimId: String): List<com.odysee.app.core.data.memberships.MembershipTier>
    suspend fun createMembership(
        channelClaimId: String,
        name: String,
        description: String?,
        priceUsd: Double,
    ): com.odysee.app.core.data.memberships.MembershipTier?
    suspend fun updateMembership(
        id: String,
        name: String?,
        description: String?,
        priceUsd: Double?,
    ): com.odysee.app.core.data.memberships.MembershipTier?
    suspend fun deleteMembership(id: String)
    suspend fun getChannelStats(channelClaimId: String): com.odysee.app.core.data.analytics.ChannelStats?
    suspend fun checkCreatorMembershipsForChannels(
        creatorChannelId: String,
        channelClaimIds: List<String>,
    ): Map<String, String>
    suspend fun listMyMembershipSubscriptions(): List<com.odysee.app.core.data.memberships.MySubscription>

    suspend fun updateChannel(
        claimId: String,
        title: String? = null,
        description: String? = null,
        thumbnailUrl: String? = null,
        coverUrl: String? = null,
        websiteUrl: String? = null,
        email: String? = null,
        tags: List<String>? = null,
        languages: List<String>? = null,
        featured: List<String>? = null,
        replace: Boolean = false,
    ): String
    suspend fun uploadImage(bytes: ByteArray, fileName: String, mimeType: String): String
    suspend fun publishStream(
        file: java.io.File,
        authToken: String,
        params: com.odysee.app.core.data.publish.PublishParams,
        onProgress: (uploadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): String
    suspend fun repost(name: String, claimId: String, channelId: String, bidLbc: Double): String

    suspend fun createChannel(
        name: String,
        bidLbc: Double,
        title: String? = null,
        description: String? = null,
        thumbnailUrl: String? = null,
        coverUrl: String? = null,
        websiteUrl: String? = null,
        email: String? = null,
        tags: List<String>? = null,
        languages: List<String>? = null,
    ): String

    data class OwnCommentsPage(
        val comments: List<com.odysee.app.core.model.Comment>,
        val claimsById: Map<String, com.odysee.app.core.model.Claim>,
        val page: Int,
        val totalPages: Int,
        val totalItems: Int,
    )

    suspend fun listOwnComments(
        channelClaimId: String,
        channelName: String,
        page: Int,
        pageSize: Int = 10,
    ): OwnCommentsPage

    suspend fun editOwnComment(
        commentId: String,
        newBody: String,
        channelClaimId: String,
        channelName: String,
    )

    suspend fun deleteOwnComment(
        commentId: String,
        channelClaimId: String,
        channelName: String,
    )

    data class PurchasesPage(
        val items: List<com.odysee.app.core.model.Claim>,
        val page: Int,
        val totalPages: Int,
        val totalItems: Int,
    )

    suspend fun listPurchases(page: Int, pageSize: Int = 10): PurchasesPage

    /**
     * Pure-SDK LBC paywall purchase. Debits the wallet, server records a
     * purchase_receipt so subsequent resolves include playback. Returns the tx id.
     */
    suspend fun purchaseClaimWithLbc(claimId: String): String

    /**
     * Lbryio fiat (Arweave-backed) purchase check. Returns true if the signed-in
     * user has at least one valid purchase / rental transaction for this claim.
     */
    suspend fun hasFiatPurchase(claimId: String): Boolean
}

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val sdkProxyApi: SdkProxyApi,
    private val odyseeContentApi: OdyseeContentApi,
    private val commentronApi: CommentronApi,
    private val lighthouseApi: LighthouseApi,
    private val lbryioApi: LbryioApi,
    private val livestreamApi: LivestreamApi,
    private val okHttpClient: okhttp3.OkHttpClient,
) : ContentRepository {

    override suspend fun publishStream(
        file: java.io.File,
        authToken: String,
        params: com.odysee.app.core.data.publish.PublishParams,
        onProgress: (uploadedBytes: Long, totalBytes: Long) -> Unit,
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val tusClient = io.tus.java.client.TusClient()
        tusClient.uploadCreationURL = java.net.URL("https://api.na-backend.odysee.com/api/v2/publish/")
        tusClient.setHeaders(mapOf("X-Lbry-Auth-Token" to authToken))

        val upload = io.tus.java.client.TusUpload(file)
        val uploader = tusClient.createUpload(upload)
        uploader.chunkSize = 1024 * 1024
        val total = upload.size
        try {
            while (uploader.uploadChunk() > -1) {
                onProgress(uploader.offset, total)
            }
        } finally {
            uploader.finish()
        }

        val notifyUrl = uploader.uploadURL.toString() + "/notify"
        val tagSet = LinkedHashSet(params.tags)
        when (params.visibility) {
            com.odysee.app.core.data.publish.PublishVisibility.Private -> tagSet += "c:private"
            com.odysee.app.core.data.publish.PublishVisibility.Unlisted -> tagSet += "c:unlisted"
            com.odysee.app.core.data.publish.PublishVisibility.Scheduled ->
                tagSet += if (params.scheduledShow) "c:scheduled:show" else "c:scheduled:hide"
            com.odysee.app.core.data.publish.PublishVisibility.Public -> Unit
        }
        if (params.membersOnly) tagSet += "c:members-only"
        if (params.visibility == com.odysee.app.core.data.publish.PublishVisibility.Public) {
            params.fiatPurchaseUsd?.toDoubleOrNull()?.takeIf { it > 0 }?.let { amt ->
                tagSet += "c:purchase"
                tagSet += "c:purchase:" + "%.2f".format(amt)
            }
            params.fiatRentalUsd?.toDoubleOrNull()?.takeIf { it > 0 }?.let { amt ->
                params.fiatRentalSeconds?.takeIf { it > 0 }?.let { secs ->
                    tagSet += "c:rental"
                    tagSet += "c:rental:" + "%.2f".format(amt) + ":" + secs
                }
            }
        }
        val publishParams = kotlinx.serialization.json.buildJsonObject {
            put("name", kotlinx.serialization.json.JsonPrimitive(params.name))
            put("title", kotlinx.serialization.json.JsonPrimitive(params.title))
            put("bid", kotlinx.serialization.json.JsonPrimitive(params.bid))
            params.description?.let { put("description", kotlinx.serialization.json.JsonPrimitive(it)) }
            params.channelId?.let { put("channel_id", kotlinx.serialization.json.JsonPrimitive(it)) }
            params.thumbnailUrl?.let { put("thumbnail_url", kotlinx.serialization.json.JsonPrimitive(it)) }
            if (tagSet.isNotEmpty()) {
                put(
                    "tags",
                    kotlinx.serialization.json.JsonArray(
                        tagSet.map { kotlinx.serialization.json.JsonPrimitive(it) },
                    ),
                )
            }
            if (params.languages.isNotEmpty()) {
                put(
                    "languages",
                    kotlinx.serialization.json.JsonArray(
                        params.languages.map { kotlinx.serialization.json.JsonPrimitive(it) },
                    ),
                )
            }
            params.license?.let { put("license", kotlinx.serialization.json.JsonPrimitive(it)) }
            params.licenseUrl?.let { put("license_url", kotlinx.serialization.json.JsonPrimitive(it)) }
            if (params.locations.isNotEmpty()) {
                put(
                    "locations",
                    kotlinx.serialization.json.JsonArray(
                        params.locations.map { kotlinx.serialization.json.JsonPrimitive(it) },
                    ),
                )
            }
            params.releaseTime?.let { put("release_time", kotlinx.serialization.json.JsonPrimitive(it)) }
            // LBC paywall — only for public claims, matching web.
            if (params.visibility == com.odysee.app.core.data.publish.PublishVisibility.Public) {
                params.feeAmountLbc?.toDoubleOrNull()?.takeIf { it > 0 }?.let { amt ->
                    put("fee_currency", kotlinx.serialization.json.JsonPrimitive("LBC"))
                    put("fee_amount", kotlinx.serialization.json.JsonPrimitive(params.feeAmountLbc))
                }
            }
            put("blocking", kotlinx.serialization.json.JsonPrimitive(true))
        }
        val rpcBody = kotlinx.serialization.json.buildJsonObject {
            put("jsonrpc", kotlinx.serialization.json.JsonPrimitive("2.0"))
            put("method", kotlinx.serialization.json.JsonPrimitive("publish"))
            put("params", publishParams)
            put(
                "counter",
                kotlinx.serialization.json.JsonPrimitive(System.currentTimeMillis() / 1000),
            )
        }.toString()
        val request = okhttp3.Request.Builder()
            .url(notifyUrl)
            .post(rpcBody.toRequestBody("application/json".toMediaType()))
            .addHeader("X-Lbry-Auth-Token", authToken)
            .addHeader("Tus-Resumable", "1.0.0")
            .build()
        val resp = okHttpClient.newCall(request).execute()
        resp.use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) error("Publish HTTP ${r.code}: $text")
            val parsed = runCatching {
                kotlinx.serialization.json.Json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject
            }.getOrNull() ?: error("Unexpected publish response")
            val result = parsed["result"] as? kotlinx.serialization.json.JsonObject
                ?: run {
                    val err = (parsed["error"] as? kotlinx.serialization.json.JsonObject)
                        ?.get("message") as? kotlinx.serialization.json.JsonPrimitive
                    error(err?.contentOrNull ?: "Publish failed")
                }
            val outputs = result["outputs"] as? kotlinx.serialization.json.JsonArray
            val firstOutput = outputs?.firstOrNull() as? kotlinx.serialization.json.JsonObject
            val claimId = (firstOutput?.get("claim_id") as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
            return@withContext claimId ?: error("Publish: no claim id returned")
        }
    }

    override suspend fun uploadImage(bytes: ByteArray, fileName: String, mimeType: String): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val body = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart(
                    "file-input",
                    fileName,
                    bytes.toRequestBody(mimeType.toMediaType()),
                )
                .addFormDataPart("name", fileName.substringBeforeLast('.', fileName))
                .build()
            val request = okhttp3.Request.Builder()
                .url("https://thumbs.odycdn.com/upload")
                .post(body)
                .build()
            okHttpClient.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("HTTP ${resp.code}: $text")
                val obj = runCatching {
                    kotlinx.serialization.json.Json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject
                }.getOrNull() ?: error("Unexpected response")
                val type = (obj["type"] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                if (type != "success") {
                    error((obj["message"] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull ?: "Upload failed")
                }
                (obj["message"] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                    ?: error("Missing URL in response")
            }
        }

    override suspend fun getViewCounts(claimIds: List<String>): Map<String, Long> {
        if (claimIds.isEmpty()) return emptyMap()
        val unique = claimIds.distinct()
        val env = runCatching { lbryioApi.viewCount(unique.joinToString(",")) }.getOrNull()
        val counts = env?.data ?: return emptyMap()
        return unique.mapIndexedNotNull { i, id -> counts.getOrNull(i)?.let { id to it } }.toMap()
    }

    override suspend fun getHomepage(lang: String): Homepage {
        val envelope = odyseeContentApi.getHomepage(lang)
        val langData = envelope.data[lang]
            ?: throw IllegalStateException("Homepage data missing for lang=$lang")
        return langData.toDomain()
    }

    override suspend fun getCategoryFeed(
        category: HomepageCategory,
        page: Int,
        homepageLanguage: String?,
    ): List<Claim> {
        val notTags = buildList {
            add("mature")
            if (category.excludeShorts) add("short")
        }
        val releaseTimeFilter = category.daysOfContent?.let { days ->
            val cutoff = (System.currentTimeMillis() / 1000) - (days.toLong() * 86_400L)
            ">$cutoff"
        }
        val effectiveLanguages = category.languages.takeIf { it.isNotEmpty() }
            ?: homepageLanguage?.takeIf { it.isNotBlank() }?.let { listOf(it) }
        val params = ClaimSearchParams(
            page = page.coerceAtLeast(1),
            pageSize = category.pageSize.coerceIn(1, 50),
            channelIds = category.channelIds.takeIf { it.isNotEmpty() },
            limitClaimsPerChannel = category.channelLimit,
            releaseTime = releaseTimeFilter,
            notTags = notTags,
            anyLanguages = effectiveLanguages,
        )
        val response = sdkProxyApi.claimSearch(
            JsonRpcRequest(method = "claim_search", params = params),
        )
        return response.unwrap().items.map { it.toDomain() }
    }

    override suspend fun resolveStreamUrl(uri: String): String {
        val response = sdkProxyApi.get(
            JsonRpcRequest(method = "get", params = GetStreamParams(uri = uri)),
        )
        return response.unwrap().streamingUrl
            ?: throw IllegalStateException("No streaming_url returned for $uri")
    }

    override suspend fun getComments(
        claimId: String,
        page: Int,
        pageSize: Int,
        sortBy: Int,
        parentId: String?,
    ): List<Comment> {
        val response = commentronApi.commentList(
            JsonRpcRequest(
                method = "comment.List",
                params = CommentListParams(
                    claimId = claimId,
                    page = page,
                    pageSize = pageSize,
                    topLevel = parentId == null,
                    parentId = parentId,
                    sortBy = sortBy,
                ),
            ),
        )
        return response.unwrap().items.map { it.toDomain() }
    }

    override suspend fun getCommentReactions(
        commentIds: List<String>,
        myChannelId: String?,
        myChannelName: String?,
    ): Map<String, CommentReactionCounts> {
        if (commentIds.isEmpty()) return emptyMap()
        val params = if (!myChannelId.isNullOrBlank() && !myChannelName.isNullOrBlank()) {
            val sign = runCatching {
                sdkProxyApi.channelSign(
                    JsonRpcRequest(
                        method = "channel_sign",
                        params = ChannelSignParams(
                            channelId = myChannelId,
                            hexdata = myChannelName
                                .encodeToByteArray()
                                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
                        ),
                    ),
                ).unwrap()
            }.getOrNull()
            CommentReactListParams(
                commentIds = commentIds.joinToString(","),
                channelId = myChannelId,
                channelName = myChannelName,
                signature = sign?.signature,
                signingTs = sign?.signingTs,
            )
        } else {
            CommentReactListParams(commentIds = commentIds.joinToString(","))
        }
        val response = runCatching {
            commentronApi.commentReactList(
                JsonRpcRequest(method = "reaction.List", params = params),
            ).unwrap()
        }.getOrNull() ?: return emptyMap()
        return commentIds.associateWith { id ->
            val others = response.othersReactions[id] ?: emptyMap()
            val mine = response.myReactions[id] ?: emptyMap()
            CommentReactionCounts(
                likes = (others["like"] ?: 0) + (mine["like"] ?: 0),
                dislikes = (others["dislike"] ?: 0) + (mine["dislike"] ?: 0),
                myLike = (mine["like"] ?: 0) > 0,
                myDislike = (mine["dislike"] ?: 0) > 0,
                creatorLike = ((others["creator_like"] ?: 0) + (mine["creator_like"] ?: 0)) > 0,
            )
        }
    }

    override suspend fun reactToComment(
        commentId: String,
        channelId: String,
        channelName: String,
        type: String,
        remove: Boolean,
    ) {
        val hex = channelName.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = channelId, hexdata = hex)),
        ).unwrap()
        commentronApi.commentReact(
            JsonRpcRequest(
                method = "reaction.React",
                params = CommentReactParams(
                    commentIds = commentId,
                    channelId = channelId,
                    channelName = channelName,
                    signature = sign.signature ?: error("channel_sign no signature"),
                    signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                    type = type,
                    remove = remove,
                    clear_types = if (!remove && type == "like") "dislike" else if (!remove && type == "dislike") "like" else null,
                ),
            ),
        )
    }

    override suspend fun pinComment(
        commentId: String,
        channelId: String,
        channelName: String,
        remove: Boolean,
    ) {
        val hex = commentId.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = channelId, hexdata = hex)),
        ).unwrap()
        commentronApi.commentPin(
            JsonRpcRequest(
                method = "comment.Pin",
                params = CommentPinParams(
                    commentId = commentId,
                    channelId = channelId,
                    channelName = channelName,
                    signature = sign.signature ?: error("channel_sign no signature"),
                    signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                    remove = remove,
                ),
            ),
        )
    }

    override suspend fun editComment(
        commentId: String,
        text: String,
        channelId: String,
        channelName: String,
    ): Comment {
        val hex = text.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = channelId, hexdata = hex)),
        ).unwrap()
        val resp = commentronApi.commentEdit(
            JsonRpcRequest(
                method = "comment.Edit",
                params = CommentEditParams(
                    commentId = commentId,
                    comment = text,
                    channelId = channelId,
                    channelName = channelName,
                    signature = sign.signature ?: error("channel_sign no signature"),
                    signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                ),
            ),
        ).unwrap()
        return resp.toDomain()
    }

    override suspend fun deleteComment(commentId: String, channelId: String, channelName: String) {
        val hex = commentId.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = channelId, hexdata = hex)),
        ).unwrap()
        commentronApi.commentAbandon(
            JsonRpcRequest(
                method = "comment.Abandon",
                params = CommentAbandonParams(
                    commentId = commentId,
                    channelId = channelId,
                    channelName = channelName,
                    signature = sign.signature,
                    signingTs = sign.signingTs,
                ),
            ),
        )
    }

    override suspend fun blockCommenter(
        modChannelId: String,
        modChannelName: String,
        blockedChannelId: String,
        blockedChannelName: String,
        creatorChannelId: String?,
        creatorChannelName: String?,
        offendingCommentId: String?,
        blockAll: Boolean,
        timeoutSec: Long?,
    ) {
        val hex = modChannelName.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = modChannelId, hexdata = hex)),
        ).unwrap()
        commentronApi.moderationBlock(
            JsonRpcRequest(
                method = "moderation.Block",
                params = com.odysee.app.core.network.dto.ModerationBlockParams(
                    modChannelId = modChannelId,
                    modChannelName = modChannelName,
                    blockedChannelId = blockedChannelId,
                    blockedChannelName = blockedChannelName,
                    creatorChannelId = creatorChannelId,
                    creatorChannelName = creatorChannelName,
                    offendingCommentId = offendingCommentId,
                    blockAll = if (blockAll) true else null,
                    timeOut = timeoutSec,
                    signature = sign.signature ?: error("channel_sign no signature"),
                    signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                ),
            ),
        )
    }

    override suspend fun addCommentModerator(
        modChannelId: String,
        modChannelName: String,
        creatorChannelId: String,
        creatorChannelName: String,
    ) {
        val hex = creatorChannelName.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = creatorChannelId, hexdata = hex)),
        ).unwrap()
        commentronApi.moderationAddDelegate(
            JsonRpcRequest(
                method = "moderation.AddDelegate",
                params = com.odysee.app.core.network.dto.ModerationAddDelegateParams(
                    modChannelId = modChannelId,
                    modChannelName = modChannelName,
                    channelId = creatorChannelId,
                    channelName = creatorChannelName,
                    signature = sign.signature ?: error("channel_sign no signature"),
                    signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                ),
            ),
        )
    }

    override suspend fun removeCommentModerator(
        modChannelId: String,
        modChannelName: String,
        creatorChannelId: String,
        creatorChannelName: String,
    ) {
        val hex = creatorChannelName.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = creatorChannelId, hexdata = hex)),
        ).unwrap()
        commentronApi.moderationRemoveDelegate(
            JsonRpcRequest(
                method = "moderation.RemoveDelegate",
                params = com.odysee.app.core.network.dto.ModerationRemoveDelegateParams(
                    modChannelId = modChannelId,
                    modChannelName = modChannelName,
                    channelId = creatorChannelId,
                    channelName = creatorChannelName,
                    signature = sign.signature ?: error("channel_sign no signature"),
                    signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                ),
            ),
        )
    }

    override suspend fun listBlockedCommenters(
        creatorChannelId: String,
        creatorChannelName: String,
    ): List<com.odysee.app.core.data.moderation.BlockedCommenter> {
        val hex = creatorChannelName.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = creatorChannelId, hexdata = hex)),
        ).unwrap()
        val response = runCatching {
            commentronApi.moderationBlockedList(
                JsonRpcRequest(
                    method = "moderation.BlockedList",
                    params = com.odysee.app.core.network.dto.ModerationBlockedListParams(
                        modChannelId = creatorChannelId,
                        modChannelName = creatorChannelName,
                        creatorChannelId = creatorChannelId,
                        creatorChannelName = creatorChannelName,
                        signature = sign.signature ?: error("channel_sign no signature"),
                        signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                    ),
                ),
            ).unwrap()
        }.getOrNull() ?: return emptyList()
        val result = mutableListOf<com.odysee.app.core.data.moderation.BlockedCommenter>()
        response.blockedChannels.forEach {
            result.add(
                com.odysee.app.core.data.moderation.BlockedCommenter(
                    claimId = it.blockedChannelId,
                    name = it.blockedChannelName,
                    blockedAt = it.blockedAt,
                    bannedUntil = it.bannedUntil,
                    scope = com.odysee.app.core.data.moderation.BlockScope.Channel,
                ),
            )
        }
        response.globallyBlockedChannels.forEach {
            result.add(
                com.odysee.app.core.data.moderation.BlockedCommenter(
                    claimId = it.blockedChannelId,
                    name = it.blockedChannelName,
                    blockedAt = it.blockedAt,
                    bannedUntil = it.bannedUntil,
                    scope = com.odysee.app.core.data.moderation.BlockScope.Global,
                ),
            )
        }
        response.delegatedBlockedChannels.forEach {
            result.add(
                com.odysee.app.core.data.moderation.BlockedCommenter(
                    claimId = it.blockedChannelId,
                    name = it.blockedChannelName,
                    blockedAt = it.blockedAt,
                    bannedUntil = it.bannedUntil,
                    scope = com.odysee.app.core.data.moderation.BlockScope.Delegated,
                ),
            )
        }
        return result
    }

    override suspend fun listCommentModerators(
        creatorChannelId: String,
        creatorChannelName: String,
    ): List<com.odysee.app.core.data.moderation.CommentModerator> {
        val hex = creatorChannelName.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = creatorChannelId, hexdata = hex)),
        ).unwrap()
        val response = runCatching {
            commentronApi.moderationListDelegates(
                JsonRpcRequest(
                    method = "moderation.ListDelegates",
                    params = com.odysee.app.core.network.dto.ModerationListDelegatesParams(
                        creatorChannelId = creatorChannelId,
                        creatorChannelName = creatorChannelName,
                        signature = sign.signature ?: error("channel_sign no signature"),
                        signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                    ),
                ),
            ).unwrap()
        }.getOrNull() ?: return emptyList()
        return response.delegates.map {
            com.odysee.app.core.data.moderation.CommentModerator(
                claimId = it.channelId,
                name = it.channelName,
            )
        }
    }

    override suspend fun unblockCommenter(
        modChannelId: String,
        modChannelName: String,
        blockedChannelId: String,
        blockedChannelName: String,
        creatorChannelId: String?,
        creatorChannelName: String?,
    ) {
        val hex = modChannelName.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = modChannelId, hexdata = hex)),
        ).unwrap()
        commentronApi.moderationUnblock(
            JsonRpcRequest(
                method = "moderation.UnBlock",
                params = com.odysee.app.core.network.dto.ModerationBlockParams(
                    modChannelId = modChannelId,
                    modChannelName = modChannelName,
                    blockedChannelId = blockedChannelId,
                    blockedChannelName = blockedChannelName,
                    creatorChannelId = creatorChannelId,
                    creatorChannelName = creatorChannelName,
                    signature = sign.signature ?: error("channel_sign no signature"),
                    signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                ),
            ),
        )
    }

    override suspend fun getChannel(claimId: String): Channel? {
        val response = sdkProxyApi.claimSearch(
            JsonRpcRequest(
                method = "claim_search",
                params = ClaimSearchParams(
                    claimType = listOf("channel"),
                    claimIds = listOf(claimId),
                    pageSize = 1,
                    streamTypes = null,
                    hasSource = null,
                    notTags = null,
                ),
            ),
        )
        return response.unwrap().items.firstOrNull()?.toChannel()
    }

    override suspend fun getChannels(claimIds: List<String>): List<Channel> {
        if (claimIds.isEmpty()) return emptyList()
        val unique = claimIds.distinct()
        val collected = mutableListOf<Channel>()
        unique.chunked(50).forEach { chunk ->
            val response = runCatching {
                sdkProxyApi.claimSearch(
                    JsonRpcRequest(
                        method = "claim_search",
                        params = ClaimSearchParams(
                            claimType = listOf("channel"),
                            claimIds = chunk,
                            pageSize = chunk.size.coerceAtMost(50),
                            streamTypes = null,
                            hasSource = null,
                            notTags = null,
                        ),
                    ),
                )
            }.getOrNull() ?: return@forEach
            collected.addAll(response.unwrap().items.map { it.toChannel() })
        }
        return collected
    }

    override suspend fun getClaimsByIds(claimIds: List<String>): List<Claim> {
        if (claimIds.isEmpty()) return emptyList()
        val unique = claimIds.distinct()
        val collected = mutableListOf<Claim>()
        unique.chunked(50).forEach { chunk ->
            val response = runCatching {
                sdkProxyApi.claimSearch(
                    JsonRpcRequest(
                        method = "claim_search",
                        params = ClaimSearchParams(
                            claimIds = chunk,
                            pageSize = chunk.size.coerceAtMost(50),
                            streamTypes = null,
                            hasSource = null,
                            notTags = null,
                            orderBy = listOf("name"),
                        ),
                    ),
                )
            }.getOrNull() ?: return@forEach
            collected.addAll(response.unwrap().items.map { it.toDomain() })
        }
        return collected
    }

    override suspend fun getChannelCollections(channelClaimId: String): List<com.odysee.app.core.network.dto.CollectionClaimDto> {
        val response = runCatching {
            sdkProxyApi.claimSearch(
                JsonRpcRequest(
                    method = "claim_search",
                    params = ClaimSearchParams(
                        claimType = listOf("collection"),
                        channelIds = listOf(channelClaimId),
                        pageSize = 50,
                        streamTypes = null,
                        hasSource = null,
                        notTags = null,
                        orderBy = listOf("release_time"),
                    ),
                ),
            )
        }.getOrNull() ?: return emptyList()
        val items = response.unwrap().items
        return items.map { c ->
            com.odysee.app.core.network.dto.CollectionClaimDto(
                claimId = c.claimId,
                name = c.name,
                permanentUrl = c.permanentUrl,
                canonicalUrl = c.canonicalUrl,
                timestamp = c.timestamp,
                value = c.value?.let { v ->
                    com.odysee.app.core.network.dto.CollectionValueDto(
                        title = v.title,
                        description = v.description,
                        thumbnail = v.thumbnail,
                        claims = v.claims,
                    )
                },
                signingChannel = null,
            )
        }
    }

    override suspend fun getChannelVideos(
        channelClaimId: String,
        page: Int,
        pageSize: Int,
    ): List<Claim> {
        val response = sdkProxyApi.claimSearch(
            JsonRpcRequest(
                method = "claim_search",
                params = ClaimSearchParams(
                    channelIds = listOf(channelClaimId),
                    page = page,
                    pageSize = pageSize,
                ),
            ),
        )
        return response.unwrap().items.map { it.toDomain() }
    }

    override suspend fun search(query: String, size: Int, from: Int): List<Claim> {
        if (query.isBlank()) return emptyList()
        val hits = lighthouseApi.search(query = query, size = size.coerceIn(1, 50), from = from.coerceAtLeast(0))
        if (hits.isEmpty()) return emptyList()
        val claimIds = hits.map { it.claimId }
        val response = sdkProxyApi.claimSearch(
            JsonRpcRequest(
                method = "claim_search",
                params = ClaimSearchParams(
                    claimIds = claimIds,
                    pageSize = claimIds.size.coerceAtMost(50),
                    claimType = listOf("stream", "channel"),
                    streamTypes = null,
                    hasSource = null,
                    notTags = null,
                ),
            ),
        )
        val claims = response.unwrap().items.map { it.toDomain() }
        val byClaimId = claims.associateBy { it.claimId }
        return claimIds.mapNotNull { byClaimId[it] }
    }

    override suspend fun getRelated(
        channelClaimId: String?,
        excludeClaimId: String,
        query: String?,
    ): List<Claim> {
        // 1. Try other videos from the same channel — most reliable source of related content.
        val channelResults = channelClaimId?.let { channelId ->
            runCatching {
                val response = sdkProxyApi.claimSearch(
                    JsonRpcRequest(
                        method = "claim_search",
                        params = ClaimSearchParams(
                            channelIds = listOf(channelId),
                            pageSize = 25,
                            orderBy = listOf("release_time"),
                        ),
                    ),
                )
                response.unwrap().items.map { it.toDomain() }.filterNot { it.claimId == excludeClaimId }
            }.getOrNull().orEmpty()
        }.orEmpty()

        // 2. Augment with a Lighthouse search by title if we have one — covers cross-channel related.
        val searchResults = if (!query.isNullOrBlank()) {
            runCatching { search(query, size = 25, from = 0) }.getOrNull()
                ?.filterNot { it.claimId == excludeClaimId }
                ?.filterNot { c -> channelResults.any { it.claimId == c.claimId } }
                .orEmpty()
        } else emptyList()

        // 3. If both empty, fall back to trending mixed feed as a generic recommended list.
        if (channelResults.isEmpty() && searchResults.isEmpty()) {
            return runCatching {
                val response = sdkProxyApi.claimSearch(
                    JsonRpcRequest(
                        method = "claim_search",
                        params = ClaimSearchParams(
                            pageSize = 25,
                            orderBy = listOf("trending_mixed"),
                            notTags = listOf("mature"),
                        ),
                    ),
                )
                response.unwrap().items.map { it.toDomain() }.filterNot { it.claimId == excludeClaimId }
            }.getOrNull().orEmpty()
        }
        return channelResults + searchResults
    }

    override suspend fun postComment(
        claimId: String,
        channelId: String,
        channelName: String,
        text: String,
        parentId: String?,
        supportTxId: String?,
    ): Comment {
        val hex = text.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = channelId, hexdata = hex)),
        ).unwrap()
        val signature = sign.signature ?: error("channel_sign returned no signature")
        val signingTs = sign.signingTs ?: error("channel_sign returned no signing_ts")
        val resp = commentronApi.commentCreate(
            JsonRpcRequest(
                method = "comment.Create",
                params = CommentCreateParams(
                    claimId = claimId,
                    channelId = channelId,
                    channelName = channelName,
                    comment = text,
                    signature = signature,
                    signingTs = signingTs,
                    parentId = parentId,
                    supportTxId = supportTxId,
                ),
            ),
        )
        return resp.unwrap().toDomain()
    }

    override suspend fun repost(
        name: String,
        claimId: String,
        channelId: String,
        bidLbc: Double,
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val bid = "%.4f".format(bidLbc).trimEnd('0').trimEnd('.').ifEmpty { "0.001" }
        val cleanName = name.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(100)
            .ifBlank { "repost-" + claimId.take(8) }
        val res = sdkProxyApi.streamRepost(
            com.odysee.app.core.network.jsonrpc.JsonRpcRequest(
                method = "stream_repost",
                params = com.odysee.app.core.network.dto.StreamRepostParams(
                    name = cleanName,
                    bid = bid,
                    claimId = claimId,
                    channelId = channelId,
                ),
            ),
        ).unwrap()
        val outputs = res["outputs"] as? kotlinx.serialization.json.JsonArray
        val first = outputs?.firstOrNull() as? kotlinx.serialization.json.JsonObject
        (first?.get("claim_id") as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
            ?: error("Repost: no claim id returned")
    }

    override suspend fun createChannel(
        name: String,
        bidLbc: Double,
        title: String?,
        description: String?,
        thumbnailUrl: String?,
        coverUrl: String?,
        websiteUrl: String?,
        email: String?,
        tags: List<String>?,
        languages: List<String>?,
    ): String {
        val bid = "%.4f".format(bidLbc).trimEnd('0').trimEnd('.').ifEmpty { "0.001" }
        val cleanName = name.trim().let { if (it.startsWith("@")) it else "@$it" }
        val res = sdkProxyApi.channelCreate(
            JsonRpcRequest(
                method = "channel_create",
                params = com.odysee.app.core.network.dto.ChannelCreateParams(
                    name = cleanName,
                    bid = bid,
                    title = title,
                    description = description,
                    thumbnail_url = thumbnailUrl,
                    cover_url = coverUrl,
                    website_url = websiteUrl,
                    email = email,
                    tags = tags,
                    languages = languages,
                    blocking = true,
                ),
            ),
        ).unwrap()
        return res.txid ?: error("channel_create returned no txid")
    }

    override suspend fun updateChannel(
        claimId: String,
        title: String?,
        description: String?,
        thumbnailUrl: String?,
        coverUrl: String?,
        websiteUrl: String?,
        email: String?,
        tags: List<String>?,
        languages: List<String>?,
        featured: List<String>?,
        replace: Boolean,
    ): String {
        val res = sdkProxyApi.channelUpdate(
            JsonRpcRequest(
                method = "channel_update",
                params = com.odysee.app.core.network.dto.ChannelUpdateParams(
                    claimId = claimId,
                    title = title,
                    description = description,
                    thumbnail_url = thumbnailUrl,
                    cover_url = coverUrl,
                    website_url = websiteUrl,
                    email = email,
                    tags = tags,
                    languages = languages,
                    featured = featured,
                    clear_featured = if (featured != null && featured.isEmpty()) true else null,
                    replace = if (replace) true else null,
                    blocking = true,
                ),
            ),
        ).unwrap()
        return res.txid ?: error("channel_update returned no txid")
    }

    override suspend fun getFeaturedChannelSections(
        channelClaimId: String,
    ): List<com.odysee.app.core.data.featured.FeaturedChannelSection> {
        val response = runCatching {
            commentronApi.settingGet(
                JsonRpcRequest(
                    method = "setting.Get",
                    params = com.odysee.app.core.network.dto.SettingGetParams(channelId = channelClaimId),
                ),
            ).unwrap()
        }.getOrNull() ?: return emptyList()
        return response.channelSections?.entries.orEmpty().mapNotNull { (key, entry) ->
            if (entry.valueType != "featured_channels") return@mapNotNull null
            com.odysee.app.core.data.featured.FeaturedChannelSection(
                id = entry.id ?: key,
                title = entry.value?.title.orEmpty(),
                uris = entry.value?.uris.orEmpty(),
            )
        }
    }

    override suspend fun updateFeaturedChannelSections(
        channelClaimId: String,
        channelName: String,
        sections: List<com.odysee.app.core.data.featured.FeaturedChannelSection>,
    ) {
        val hex = channelName.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = channelClaimId, hexdata = hex)),
        ).unwrap()
        val existing = runCatching {
            commentronApi.settingGet(
                JsonRpcRequest(
                    method = "setting.Get",
                    params = com.odysee.app.core.network.dto.SettingGetParams(channelId = channelClaimId),
                ),
            ).unwrap()
        }.getOrNull()
        val existingEntries: Map<String, com.odysee.app.core.network.dto.ChannelSectionEntryDto> =
            existing?.channelSections?.entries.orEmpty()
        val nonFeatured = existingEntries.filter { (_, e) -> e.valueType != "featured_channels" }
        val featuredJson = buildMap<String, kotlinx.serialization.json.JsonElement> {
            nonFeatured.forEach { (k, e) ->
                put(k, kotlinx.serialization.json.Json.encodeToJsonElement(
                    com.odysee.app.core.network.dto.ChannelSectionEntryDto.serializer(), e,
                ))
            }
            sections.forEach { s ->
                put(
                    s.id,
                    kotlinx.serialization.json.buildJsonObject {
                        put("id", kotlinx.serialization.json.JsonPrimitive(s.id))
                        put("value_type", kotlinx.serialization.json.JsonPrimitive("featured_channels"))
                        put(
                            "value",
                            kotlinx.serialization.json.buildJsonObject {
                                put("title", kotlinx.serialization.json.JsonPrimitive(s.title))
                                put(
                                    "uris",
                                    kotlinx.serialization.json.JsonArray(
                                        s.uris.map { kotlinx.serialization.json.JsonPrimitive(it) },
                                    ),
                                )
                            },
                        )
                    },
                )
            }
        }
        val body = kotlinx.serialization.json.buildJsonObject {
            put("channel_id", kotlinx.serialization.json.JsonPrimitive(channelClaimId))
            put("channel_name", kotlinx.serialization.json.JsonPrimitive(channelName))
            put(
                "signature",
                kotlinx.serialization.json.JsonPrimitive(
                    sign.signature ?: error("channel_sign no signature"),
                ),
            )
            put(
                "signing_ts",
                kotlinx.serialization.json.JsonPrimitive(
                    sign.signingTs ?: error("channel_sign no signing_ts"),
                ),
            )
            put(
                "channel_sections",
                kotlinx.serialization.json.buildJsonObject {
                    put(
                        "entries",
                        kotlinx.serialization.json.JsonObject(featuredJson),
                    )
                },
            )
        }
        commentronApi.settingUpdate(
            JsonRpcRequest(method = "setting.Update", params = body),
        )
    }

    override suspend fun getDiscussionSettings(
        channelClaimId: String,
    ): com.odysee.app.core.data.discussion.DiscussionSettings {
        val response = runCatching {
            commentronApi.settingGet(
                JsonRpcRequest(
                    method = "setting.Get",
                    params = com.odysee.app.core.network.dto.SettingGetParams(channelId = channelClaimId),
                ),
            ).unwrap()
        }.getOrNull() ?: return com.odysee.app.core.data.discussion.DiscussionSettings()
        return com.odysee.app.core.data.discussion.DiscussionSettings(
            minTipAmountComment = response.minTipAmountComment ?: 0.0,
            minTipAmountSuperChat = response.minTipAmountSuperChat ?: 0.0,
            slowModeMinGap = response.slowModeMinGap ?: 0,
            commentsMembersOnly = response.commentsMembersOnly ?: false,
            livestreamChatMembersOnly = response.livestreamChatMembersOnly ?: false,
        )
    }

    override suspend fun updateDiscussionSettings(
        channelClaimId: String,
        channelName: String,
        settings: com.odysee.app.core.data.discussion.DiscussionSettings,
    ) {
        val hex = channelName.encodeToByteArray().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(method = "channel_sign", params = ChannelSignParams(channelId = channelClaimId, hexdata = hex)),
        ).unwrap()
        val body = kotlinx.serialization.json.buildJsonObject {
            put("channel_id", kotlinx.serialization.json.JsonPrimitive(channelClaimId))
            put("channel_name", kotlinx.serialization.json.JsonPrimitive(channelName))
            put(
                "signature",
                kotlinx.serialization.json.JsonPrimitive(
                    sign.signature ?: error("channel_sign no signature"),
                ),
            )
            put(
                "signing_ts",
                kotlinx.serialization.json.JsonPrimitive(
                    sign.signingTs ?: error("channel_sign no signing_ts"),
                ),
            )
            put("min_tip_amount_comment", kotlinx.serialization.json.JsonPrimitive(settings.minTipAmountComment))
            put("min_tip_amount_super_chat", kotlinx.serialization.json.JsonPrimitive(settings.minTipAmountSuperChat))
            put("slow_mode_min_gap", kotlinx.serialization.json.JsonPrimitive(settings.slowModeMinGap))
            put("comments_members_only", kotlinx.serialization.json.JsonPrimitive(settings.commentsMembersOnly))
            put("livestream_chat_members_only", kotlinx.serialization.json.JsonPrimitive(settings.livestreamChatMembersOnly))
        }
        commentronApi.settingUpdate(
            JsonRpcRequest(method = "setting.Update", params = body),
        )
    }

    override suspend fun listMemberships(
        channelClaimId: String,
    ): List<com.odysee.app.core.data.memberships.MembershipTier> {
        val resp = runCatching { lbryioApi.membershipList(channelClaimId) }.getOrNull()?.data
        return resp.orEmpty().map {
            com.odysee.app.core.data.memberships.MembershipTier(
                id = it.id,
                name = it.name,
                description = it.description,
                priceUsd = it.price?.amount ?: 0.0,
            )
        }
    }

    override suspend fun createMembership(
        channelClaimId: String,
        name: String,
        description: String?,
        priceUsd: Double,
    ): com.odysee.app.core.data.memberships.MembershipTier? {
        val resp = runCatching {
            lbryioApi.membershipCreate(
                channelId = channelClaimId,
                name = name,
                description = description,
                amount = priceUsd,
            )
        }.getOrNull()?.data ?: return null
        return com.odysee.app.core.data.memberships.MembershipTier(
            id = resp.id,
            name = resp.name,
            description = resp.description,
            priceUsd = resp.price?.amount ?: priceUsd,
        )
    }

    override suspend fun updateMembership(
        id: String,
        name: String?,
        description: String?,
        priceUsd: Double?,
    ): com.odysee.app.core.data.memberships.MembershipTier? {
        val resp = runCatching {
            lbryioApi.membershipUpdate(
                id = id,
                name = name,
                description = description,
                amount = priceUsd,
            )
        }.getOrNull()?.data ?: return null
        return com.odysee.app.core.data.memberships.MembershipTier(
            id = resp.id,
            name = resp.name,
            description = resp.description,
            priceUsd = resp.price?.amount ?: 0.0,
        )
    }

    override suspend fun deleteMembership(id: String) {
        runCatching { lbryioApi.membershipDelete(id) }
    }

    override suspend fun listMyMembershipSubscriptions(): List<com.odysee.app.core.data.memberships.MySubscription> {
        val resp = runCatching { lbryioApi.membershipSubscriptionList().data }.getOrNull().orEmpty()
        return resp.mapNotNull { sub ->
            val m = sub.membership ?: return@mapNotNull null
            val info = sub.subscription ?: return@mapNotNull null
            com.odysee.app.core.data.memberships.MySubscription(
                membershipId = m.id.orEmpty(),
                tierName = m.name,
                tierDescription = m.description,
                priceUsd = sub.currentPrice?.amount ?: 0.0,
                creatorChannelId = sub.creatorChannelId ?: m.channelClaimId ?: m.channelId,
                creatorChannelName = sub.creatorChannelName ?: m.channelName,
                creatorChannelUrl = sub.creatorChannelUrl,
                status = info.status,
                endsAt = info.endsAt,
            )
        }
    }

    override suspend fun checkCreatorMembershipsForChannels(
        creatorChannelId: String,
        channelClaimIds: List<String>,
    ): Map<String, String> {
        if (channelClaimIds.isEmpty()) return emptyMap()
        val resp = runCatching {
            lbryioApi.membershipCheck(
                channelId = creatorChannelId,
                claimIds = channelClaimIds.joinToString(","),
            ).data
        }.getOrNull() ?: return emptyMap()
        val out = mutableMapOf<String, String>()
        for ((channelId, list) in resp) {
            val name = list?.firstOrNull()?.name
            if (!name.isNullOrBlank()) out[channelId] = name
        }
        return out
    }

    override suspend fun getChannelStats(
        channelClaimId: String,
    ): com.odysee.app.core.data.analytics.ChannelStats? {
        val resp = runCatching { lbryioApi.channelStats(channelClaimId) }.getOrNull()?.data ?: return null
        return com.odysee.app.core.data.analytics.ChannelStats(
            subscribers = resp.channelSubs,
            subscriberChange = resp.channelSubChange,
            totalViews = resp.allContentViews,
            viewChange = resp.allContentViewChange,
            topNewVideoUri = resp.videoUriTopNew,
            topNewVideoViews = resp.videoViewsTopNew,
            topNewVideoViewChange = resp.videoViewChangeTopNew,
            topCommentedVideoUri = resp.videoUriTopCommentNew,
            topCommentedVideoComments = resp.videoCommentTopCommentNew,
            topCommentedVideoCommentChange = resp.videoCommentChangeTopCommentNew,
            topAllTimeVideoUri = resp.videoUriTopAllTime,
            topAllTimeVideoViews = resp.videoViewsTopAllTime,
            topAllTimeVideoViewChange = resp.videoViewChangeTopAllTime,
        )
    }

    override suspend fun getFeaturedChannelClaimIds(channelClaimId: String): List<String> {
        val response = runCatching {
            commentronApi.settingGet(
                JsonRpcRequest(
                    method = "setting.Get",
                    params = com.odysee.app.core.network.dto.SettingGetParams(channelId = channelClaimId),
                ),
            ).unwrap()
        }.getOrNull() ?: return emptyList()
        val sections = response.channelSections?.entries.orEmpty().values
        val uris = sections
            .filter { it.valueType == "featured_channels" }
            .flatMap { it.value?.uris.orEmpty() }
        // URIs look like `lbry://@name#claimId`; extract trailing claim id
        return uris.mapNotNull { uri ->
            val hash = uri.lastIndexOf('#')
            if (hash > 0 && hash < uri.length - 1) uri.substring(hash + 1).takeWhile { it.isLetterOrDigit() }
            else null
        }.filter { it.isNotBlank() }.distinct()
    }

    override suspend fun sendTip(claimId: String, amountLbc: Double, channelId: String?): String {
        if (amountLbc <= 0.0) error("Amount must be > 0")
        val amount = "%.4f".format(amountLbc).trimEnd('0').trimEnd('.').ifEmpty { "0.0001" }
        val res = sdkProxyApi.supportCreate(
            JsonRpcRequest(
                method = "support_create",
                params = SupportCreateParams(
                    claimId = claimId,
                    amount = amount,
                    tip = true,
                    blocking = true,
                    channelId = channelId,
                ),
            ),
        ).unwrap()
        return res.txid ?: error("support_create returned no txid")
    }

    override suspend fun getShortsFeed(page: Int, pageSize: Int): List<Claim> {
        // The SDK indexer doesn't reliably honour `content_aspect_ratio`, so we filter aspect
        // client-side via Claim.isShort and just constrain duration server-side.
        val collected = mutableListOf<Claim>()
        var p = page.coerceAtLeast(1)
        var attempts = 0
        while (collected.size < pageSize && attempts < 4) {
            val response = sdkProxyApi.claimSearch(
                JsonRpcRequest(
                    method = "claim_search",
                    params = ClaimSearchParams(
                        claimType = listOf("stream"),
                        streamTypes = listOf("video"),
                        page = p,
                        pageSize = 30,
                        orderBy = listOf("trending_mixed"),
                        notTags = listOf("mature"),
                        duration = "<=180",
                    ),
                ),
            )
            val items = response.unwrap().items
            if (items.isEmpty()) break
            collected.addAll(items.map { it.toDomain() }.filter { it.isShort })
            p++
            attempts++
        }
        return collected
    }

    override suspend fun getLivestreams(pageSize: Int): List<Claim> {
        val live = runCatching { livestreamApi.all() }.getOrNull() ?: return emptyList()
        val claimIds = live.data
            .filter { it.live == true }
            .mapNotNull { it.activeClaim?.claimId?.takeIf { id -> id.isNotBlank() } }
            .distinct()
            .take(pageSize.coerceIn(1, 50))
        if (claimIds.isEmpty()) return emptyList()
        val response = runCatching {
            sdkProxyApi.claimSearch(
                JsonRpcRequest(
                    method = "claim_search",
                    params = ClaimSearchParams(
                        claimType = listOf("stream"),
                        streamTypes = null,
                        hasSource = null,
                        claimIds = claimIds,
                        pageSize = claimIds.size.coerceAtMost(50),
                        notTags = null,
                    ),
                ),
            )
        }.getOrNull() ?: return emptyList()
        return response.unwrap().items.map { it.toDomain().copy(hasSource = false) }
    }

    override suspend fun getLivestreamUrls(): Map<String, String> {
        val live = runCatching { livestreamApi.all() }.getOrNull() ?: return emptyMap()
        return live.data
            .filter { it.live == true }
            .mapNotNull { entry ->
                val claimId = entry.activeClaim?.claimId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val url = entry.videoUrlLowLatency?.takeIf { it.isNotBlank() }
                    ?: entry.videoUrl?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                claimId to url
            }
            .toMap()
    }

    override suspend fun getUpcoming(pageSize: Int): List<Claim> {
        val now = System.currentTimeMillis() / 1000
        val response = runCatching {
            sdkProxyApi.claimSearch(
                JsonRpcRequest(
                    method = "claim_search",
                    params = ClaimSearchParams(
                        claimType = listOf("stream"),
                        streamTypes = null,
                        hasSource = false,
                        pageSize = pageSize.coerceIn(1, 50),
                        orderBy = listOf("release_time"),
                        releaseTime = ">$now",
                        notTags = listOf("mature"),
                    ),
                ),
            )
        }.getOrNull() ?: return emptyList()
        return response.unwrap().items.map { it.toDomain() }
    }

    override suspend fun getFollowingFeed(
        channelClaimIds: List<String>,
        page: Int,
        pageSize: Int,
    ): List<Claim> {
        if (channelClaimIds.isEmpty()) return emptyList()
        val response = sdkProxyApi.claimSearch(
            JsonRpcRequest(
                method = "claim_search",
                params = ClaimSearchParams(
                    channelIds = channelClaimIds,
                    page = page,
                    pageSize = pageSize.coerceIn(1, 50),
                    orderBy = listOf("release_time"),
                ),
            ),
        )
        return response.unwrap().items.map { it.toDomain() }
    }

    override suspend fun listOwnComments(
        channelClaimId: String,
        channelName: String,
        page: Int,
        pageSize: Int,
    ): ContentRepository.OwnCommentsPage {
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(
                method = "channel_sign",
                params = com.odysee.app.core.network.dto.ChannelSignParams(
                    channelId = channelClaimId,
                    hexdata = channelName
                        .encodeToByteArray()
                        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
                ),
            ),
        ).unwrap()

        val listResponse = commentronApi.commentListOwn(
            JsonRpcRequest(
                method = "comment.List",
                params = com.odysee.app.core.network.dto.CommentListOwnParams(
                    authorClaimId = channelClaimId,
                    page = page,
                    pageSize = pageSize.coerceIn(1, 50),
                    requestorChannelId = channelClaimId,
                    requestorChannelName = channelName,
                    signature = sign.signature ?: error("channel_sign no signature"),
                    signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                ),
            ),
        ).unwrap()

        val comments = listResponse.items.map { it.toDomain() }
        val claimIds = comments.mapNotNull { it.claimId }.distinct()
        val claimsById = if (claimIds.isEmpty()) emptyMap() else {
            val search = sdkProxyApi.claimSearch(
                JsonRpcRequest(
                    method = "claim_search",
                    params = ClaimSearchParams(
                        claimIds = claimIds,
                        pageSize = claimIds.size.coerceAtMost(50),
                        noTotals = true,
                    ),
                ),
            ).unwrap()
            search.items.associate { dto -> dto.claimId to dto.toDomain() }
        }
        val totalItems = listResponse.totalItems ?: comments.size
        val totalPages = listResponse.totalPages
            ?: ((totalItems + pageSize.coerceAtLeast(1) - 1) / pageSize.coerceAtLeast(1)).coerceAtLeast(1)
        return ContentRepository.OwnCommentsPage(
            comments = comments,
            claimsById = claimsById,
            page = page,
            totalPages = totalPages,
            totalItems = totalItems,
        )
    }

    override suspend fun editOwnComment(
        commentId: String,
        newBody: String,
        channelClaimId: String,
        channelName: String,
    ) {
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(
                method = "channel_sign",
                params = com.odysee.app.core.network.dto.ChannelSignParams(
                    channelId = channelClaimId,
                    hexdata = newBody.encodeToByteArray()
                        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
                ),
            ),
        ).unwrap()
        commentronApi.commentEdit(
            JsonRpcRequest(
                method = "comment.Edit",
                params = com.odysee.app.core.network.dto.CommentEditParams(
                    commentId = commentId,
                    comment = newBody,
                    channelId = channelClaimId,
                    channelName = channelName,
                    signature = sign.signature ?: error("channel_sign no signature"),
                    signingTs = sign.signingTs ?: error("channel_sign no signing_ts"),
                ),
            ),
        ).unwrap()
    }

    override suspend fun deleteOwnComment(
        commentId: String,
        channelClaimId: String,
        channelName: String,
    ) {
        val sign = sdkProxyApi.channelSign(
            JsonRpcRequest(
                method = "channel_sign",
                params = com.odysee.app.core.network.dto.ChannelSignParams(
                    channelId = channelClaimId,
                    hexdata = commentId.encodeToByteArray()
                        .joinToString("") { "%02x".format(it.toInt() and 0xff) },
                ),
            ),
        ).unwrap()
        commentronApi.commentAbandon(
            JsonRpcRequest(
                method = "comment.Abandon",
                params = com.odysee.app.core.network.dto.CommentAbandonParams(
                    commentId = commentId,
                    channelId = channelClaimId,
                    channelName = channelName,
                    signature = sign.signature,
                    signingTs = sign.signingTs,
                ),
            ),
        ).unwrap()
    }

    override suspend fun purchaseClaimWithLbc(claimId: String): String {
        // Use the canonical lbry:// URL form expected by purchase_create.
        // claim_search by claim_id gives us the canonical URL.
        val resolveResp = sdkProxyApi.claimSearch(
            JsonRpcRequest(
                method = "claim_search",
                params = ClaimSearchParams(
                    claimIds = listOf(claimId),
                    pageSize = 1,
                    noTotals = true,
                ),
            ),
        ).unwrap()
        val target = resolveResp.items.firstOrNull()
            ?: error("Claim not found")
        val url = target.canonicalUrl ?: target.permanentUrl ?: "lbry://${target.name}#${target.claimId}"
        val resp = sdkProxyApi.purchaseCreate(
            JsonRpcRequest(
                method = "purchase_create",
                params = com.odysee.app.core.network.dto.PurchaseCreateParams(
                    url = url,
                    blocking = true,
                ),
            ),
        ).unwrap()
        return resp.txid ?: error("purchase_create returned no txid")
    }

    override suspend fun hasFiatPurchase(claimId: String): Boolean {
        val resp = runCatching { lbryioApi.customerList(claimId) }.getOrNull()
        val items = resp?.data.orEmpty()
        return items.any { tx ->
            val match = tx.claimId == claimId || tx.targetClaimId == claimId
            val valid = tx.status.equals("succeeded", ignoreCase = true) ||
                tx.status.equals("active", ignoreCase = true) ||
                tx.status.isNullOrBlank() // legacy records without status
            match && valid
        }
    }

    override suspend fun listPurchases(page: Int, pageSize: Int): ContentRepository.PurchasesPage {
        val response = sdkProxyApi.purchaseList(
            JsonRpcRequest(
                method = "purchase_list",
                params = com.odysee.app.core.network.dto.PurchaseListParams(
                    page = page,
                    pageSize = pageSize.coerceIn(1, 50),
                    resolve = true,
                ),
            ),
        ).unwrap()
        val items = response.items.mapNotNull { it.claim?.toDomain() }
        return ContentRepository.PurchasesPage(
            items = items,
            page = response.page,
            totalPages = response.totalPages,
            totalItems = response.totalItems,
        )
    }
}
