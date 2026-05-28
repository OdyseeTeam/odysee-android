package com.odysee.app.core.network

import com.odysee.app.core.network.dto.LbryioEnvelope
import com.odysee.app.core.network.dto.NotificationDto
import com.odysee.app.core.network.dto.UserDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LbryioApi {

    @FormUrlEncoded
    @POST("user/new")
    suspend fun userNew(
        @Field("auth_token") authToken: String = "",
        @Field("app_id") appId: String,
        @Field("language") language: String = "en",
    ): LbryioEnvelope<UserDto>

    @FormUrlEncoded
    @POST("user/signin")
    suspend fun userSignIn(
        @Field("email") email: String,
        @Field("password") password: String? = null,
    ): LbryioEnvelope<UserDto?>

    @FormUrlEncoded
    @POST("user/signup")
    suspend fun userSignUp(
        @Field("email") email: String,
        @Field("password") password: String? = null,
    ): LbryioEnvelope<UserDto?>

    @FormUrlEncoded
    @POST("user_email/resend_token")
    suspend fun resendEmailToken(
        @Field("email") email: String,
        @Field("only_if_expired") onlyIfExpired: Boolean = true,
    ): LbryioEnvelope<UserDto?>

    @FormUrlEncoded
    @POST("user_password")
    suspend fun userPassword(
        @Field("email") email: String,
        @Field("password") password: String,
    ): LbryioEnvelope<UserDto?>

    @FormUrlEncoded
    @POST("user_password/reset")
    suspend fun userPasswordReset(
        @Field("email") email: String,
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("user_email/confirm")
    suspend fun userEmailConfirm(
        @Field("email") email: String,
        @Field("verification_token") verificationToken: String,
        @Field("auth_token") authToken: String,
    ): LbryioEnvelope<UserDto?>

    @GET("user/me")
    suspend fun userMe(): LbryioEnvelope<UserDto>

    @FormUrlEncoded
    @POST("user/has_premium")
    suspend fun userHasPremium(
        @Field("channel_claim_ids") channelClaimIds: String,
    ): LbryioEnvelope<Map<String, com.odysee.app.core.network.dto.PremiumStatusDto>>

    @FormUrlEncoded
    @POST("membership_v2/list")
    suspend fun membershipList(
        @Field("channel_claim_id") channelClaimId: String,
    ): LbryioEnvelope<List<com.odysee.app.core.network.dto.CreatorMembershipDto>>

    @FormUrlEncoded
    @POST("membership_v2/check")
    suspend fun membershipCheck(
        @Field("channel_id") channelId: String,
        @Field("claim_ids") claimIds: String,
    ): LbryioEnvelope<Map<String, List<com.odysee.app.core.network.dto.CreatorMembershipDto>?>?>

    @POST("membership_v2/subscription/list")
    suspend fun membershipSubscriptionList(): LbryioEnvelope<List<com.odysee.app.core.network.dto.MembershipSubscriptionDto>?>

    @FormUrlEncoded
    @POST("membership_v2/create")
    suspend fun membershipCreate(
        @Field("channel_id") channelId: String,
        @Field("name") name: String,
        @Field("description") description: String? = null,
        @Field("amount") amount: Double,
        @Field("currency") currency: String = "usd",
    ): LbryioEnvelope<com.odysee.app.core.network.dto.CreatorMembershipDto?>

    @FormUrlEncoded
    @POST("membership_v2/update")
    suspend fun membershipUpdate(
        @Field("id") id: String,
        @Field("name") name: String? = null,
        @Field("description") description: String? = null,
        @Field("amount") amount: Double? = null,
        @Field("currency") currency: String? = null,
    ): LbryioEnvelope<com.odysee.app.core.network.dto.CreatorMembershipDto?>

    @FormUrlEncoded
    @POST("membership_v2/delete")
    suspend fun membershipDelete(
        @Field("id") id: String,
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("channel/stats")
    suspend fun channelStats(
        @Field("claim_id") claimId: String,
    ): LbryioEnvelope<com.odysee.app.core.network.dto.ChannelStatsDto?>

    @GET("subscription/sub_count")
    suspend fun subCount(
        @Query("claim_id") claimIdCsv: String,
    ): LbryioEnvelope<List<Long>>

    @FormUrlEncoded
    @POST("subscription/new")
    suspend fun subscriptionNew(
        @Field("claim_id") claimId: String,
        @Field("channel_name") channelName: String,
        @Field("notifications_disabled") notificationsDisabled: Boolean = false,
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("subscription/delete")
    suspend fun subscriptionDelete(
        @Field("claim_id") claimId: String,
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("reaction/list")
    suspend fun reactionList(
        @Field("claim_ids") claimIds: String,
    ): LbryioEnvelope<kotlinx.serialization.json.JsonObject?>

    @FormUrlEncoded
    @POST("reaction/react")
    suspend fun reactionReact(
        @Field("claim_ids") claimIds: String,
        @Field("type") type: String,
        @Field("clear_types") clearTypes: String? = null,
        @Field("remove") remove: Boolean? = null,
    ): LbryioEnvelope<kotlinx.serialization.json.JsonElement?>

    @GET("file/view_count")
    suspend fun viewCount(
        @Query("claim_id") claimIdCsv: String,
    ): LbryioEnvelope<List<Long>>

    @GET("notification/list")
    suspend fun notificationList(
        @Query("is_app_readable") isAppReadable: Boolean = true,
    ): LbryioEnvelope<List<NotificationDto>>

    @FormUrlEncoded
    @POST("notification/edit")
    suspend fun notificationEdit(
        @Field("notification_ids") notificationIds: String,
        @Field("is_read") isRead: Boolean? = null,
        @Field("is_seen") isSeen: Boolean? = null,
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("user/invite_status")
    suspend fun userInviteStatus(
        @Field("_") unused: String = "",
    ): LbryioEnvelope<com.odysee.app.core.network.dto.InviteStatusDto?>

    @FormUrlEncoded
    @POST("user_referral_code/list")
    suspend fun userReferralCodeList(
        @Field("_") unused: String = "",
    ): LbryioEnvelope<String?>

    @FormUrlEncoded
    @POST("user/invite_new")
    suspend fun userInviteNew(
        @Field("email") email: String,
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("reward/list")
    suspend fun rewardList(
        @Field("multiple_rewards_per_type") multipleRewardsPerType: Boolean = true,
    ): LbryioEnvelope<List<com.odysee.app.core.network.dto.RewardDto>?>

    @FormUrlEncoded
    @POST("reward/claim")
    suspend fun rewardClaim(
        @Field("claim_code") claimCode: String,
    ): LbryioEnvelope<com.odysee.app.core.network.dto.RewardDto?>

    /**
     * Fiat (Arweave-backed) purchase records. Pass one or more claim_ids and
     * the server returns the transactions the signed-in user has made against
     * each. A non-empty list for a given claim_id ≈ "user has purchased".
     */
    @FormUrlEncoded
    @POST("customer/list")
    suspend fun customerList(
        @Field("claim_id_filter") claimIdFilter: String,
    ): LbryioEnvelope<List<com.odysee.app.core.network.dto.CustomerTransactionDto>?>

    /**
     * Wallet sync: ask the server for the canonical encrypted wallet blob.
     * If the server's hash differs from ours, it returns the merged blob to
     * apply locally via `sync_apply`.
     */
    @FormUrlEncoded
    @POST("sync/get")
    suspend fun syncGet(
        @Field("hash") hash: String,
    ): LbryioEnvelope<com.odysee.app.core.network.dto.SyncGetResponse?>

    /** Upload an updated encrypted wallet blob back to the server. */
    @FormUrlEncoded
    @POST("sync/set")
    suspend fun syncSet(
        @Field("old_hash") oldHash: String,
        @Field("new_hash") newHash: String,
        @Field("data") data: String,
    ): LbryioEnvelope<com.odysee.app.core.network.dto.SyncSetResponse?>

    @FormUrlEncoded
    @POST("user/delete")
    suspend fun userDelete(
        @Field("_") unused: String = "",
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("user/signout")
    suspend fun userSignout(
        @Field("_") unused: String = "",
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("notification/delete")
    suspend fun notificationDelete(
        @Field("notification_ids") notificationIds: String,
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("file/view")
    suspend fun fileView(
        @Field("uri") uri: String,
        @Field("outpoint") outpoint: String,
        @Field("claim_id") claimId: String,
        @Field("time_to_start") timeToStartMs: Long? = null,
    ): LbryioEnvelope<Boolean?>

    /** Returns `{ claim_id: position_seconds }` for the requested claims. */
    @FormUrlEncoded
    @POST("file/last_positions")
    suspend fun fileLastPositions(
        @Field("claim_ids") claimIdsCsv: String,
    ): LbryioEnvelope<Map<String, Double>?>

    @FormUrlEncoded
    @POST("user/language")
    suspend fun userLanguage(
        @Field("language") language: String,
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("user_country/set")
    suspend fun userCountrySet(
        @Field("country") country: String,
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("event/publish")
    suspend fun eventPublish(
        @Field("uri") uri: String,
        @Field("claim_id") claimId: String,
        @Field("outpoint") outpoint: String,
        @Field("channel_claim_id") channelClaimId: String? = null,
    ): LbryioEnvelope<Boolean?>

    @FormUrlEncoded
    @POST("event/search")
    suspend fun eventSearch(
        @Field("_") unused: String = "",
    ): LbryioEnvelope<Boolean?>
}
