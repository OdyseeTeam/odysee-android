package com.odysee.app.core.network

import com.odysee.app.core.network.dto.ChannelListParams
import com.odysee.app.core.network.dto.ChannelListResult
import com.odysee.app.core.network.dto.CollectionListParams
import com.odysee.app.core.network.dto.CollectionListResult
import com.odysee.app.core.network.dto.ChannelSignParams
import com.odysee.app.core.network.dto.ChannelSignResult
import com.odysee.app.core.network.dto.SupportCreateParams
import com.odysee.app.core.network.dto.SupportCreateResult
import com.odysee.app.core.network.dto.PreferenceGetParams
import com.odysee.app.core.network.dto.PreferenceSetParams
import com.odysee.app.core.network.dto.ClaimSearchParams
import com.odysee.app.core.network.dto.ClaimSearchResult
import com.odysee.app.core.network.dto.GetStreamParams
import com.odysee.app.core.network.dto.GetStreamResult
import com.odysee.app.core.network.dto.TransactionListParams
import com.odysee.app.core.network.dto.TransactionListResult
import com.odysee.app.core.network.dto.WalletBalanceParams
import com.odysee.app.core.network.dto.WalletBalanceResult
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.JsonRpcResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface SdkProxyApi {
    @POST("api/v1/proxy")
    suspend fun claimSearch(
        @Body request: JsonRpcRequest<ClaimSearchParams>,
    ): JsonRpcResponse<ClaimSearchResult>

    @POST("api/v1/proxy")
    suspend fun get(
        @Body request: JsonRpcRequest<GetStreamParams>,
    ): JsonRpcResponse<GetStreamResult>

    @POST("api/v1/proxy")
    suspend fun channelList(
        @Body request: JsonRpcRequest<ChannelListParams>,
    ): JsonRpcResponse<ChannelListResult>

    @POST("api/v1/proxy")
    suspend fun walletBalance(
        @Body request: JsonRpcRequest<WalletBalanceParams>,
    ): JsonRpcResponse<WalletBalanceResult>

    @POST("api/v1/proxy")
    suspend fun transactionList(
        @Body request: JsonRpcRequest<TransactionListParams>,
    ): JsonRpcResponse<TransactionListResult>

    @POST("api/v1/proxy")
    suspend fun addressUnused(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.AddressUnusedParams>,
    ): JsonRpcResponse<String>

    @POST("api/v1/proxy")
    suspend fun walletSend(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.WalletSendParams>,
    ): JsonRpcResponse<com.odysee.app.core.network.dto.WalletSendResult>

    @POST("api/v1/proxy")
    suspend fun streamRepost(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.StreamRepostParams>,
    ): JsonRpcResponse<kotlinx.serialization.json.JsonObject>

    @POST("api/v1/proxy")
    suspend fun preferenceGet(
        @Body request: JsonRpcRequest<PreferenceGetParams>,
    ): JsonRpcResponse<kotlinx.serialization.json.JsonObject>

    @POST("api/v1/proxy")
    suspend fun preferenceSet(
        @Body request: JsonRpcRequest<PreferenceSetParams>,
    ): JsonRpcResponse<kotlinx.serialization.json.JsonObject>

    @POST("api/v1/proxy")
    suspend fun channelSign(
        @Body request: JsonRpcRequest<ChannelSignParams>,
    ): JsonRpcResponse<ChannelSignResult>

    @POST("api/v1/proxy")
    suspend fun supportCreate(
        @Body request: JsonRpcRequest<SupportCreateParams>,
    ): JsonRpcResponse<SupportCreateResult>

    @POST("api/v1/proxy")
    suspend fun collectionList(
        @Body request: JsonRpcRequest<CollectionListParams>,
    ): JsonRpcResponse<CollectionListResult>

    @POST("api/v1/proxy")
    suspend fun channelUpdate(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.ChannelUpdateParams>,
    ): JsonRpcResponse<com.odysee.app.core.network.dto.ChannelUpdateResult>

    @POST("api/v1/proxy")
    suspend fun channelCreate(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.ChannelCreateParams>,
    ): JsonRpcResponse<com.odysee.app.core.network.dto.ChannelUpdateResult>

    @POST("api/v1/proxy")
    suspend fun purchaseList(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.PurchaseListParams>,
    ): JsonRpcResponse<com.odysee.app.core.network.dto.PurchaseListResult>

    @POST("api/v1/proxy")
    suspend fun purchaseCreate(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.PurchaseCreateParams>,
    ): JsonRpcResponse<com.odysee.app.core.network.dto.PurchaseCreateResult>

    @POST("api/v1/proxy")
    suspend fun syncHash(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.SyncHashParams>,
    ): JsonRpcResponse<String>

    @POST("api/v1/proxy")
    suspend fun syncApply(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.SyncApplyParams>,
    ): JsonRpcResponse<com.odysee.app.core.network.dto.SyncApplyResult>
}
