package com.odysee.app.core.network

import com.odysee.app.core.network.dto.CommentAbandonParams
import com.odysee.app.core.network.dto.CommentCreateParams
import com.odysee.app.core.network.dto.CommentDto
import com.odysee.app.core.network.dto.CommentEditParams
import com.odysee.app.core.network.dto.CommentListParams
import com.odysee.app.core.network.dto.CommentListResult
import com.odysee.app.core.network.dto.CommentPinParams
import com.odysee.app.core.network.dto.CommentReactListParams
import com.odysee.app.core.network.dto.CommentReactListResult
import com.odysee.app.core.network.dto.CommentReactParams
import com.odysee.app.core.network.dto.CommentReactResult
import com.odysee.app.core.network.dto.ModerationAddDelegateParams
import com.odysee.app.core.network.dto.ModerationBlockParams
import com.odysee.app.core.network.dto.ModerationRemoveDelegateParams
import com.odysee.app.core.network.jsonrpc.JsonRpcRequest
import com.odysee.app.core.network.jsonrpc.JsonRpcResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

interface CommentronApi {
    @POST("api/v2")
    suspend fun commentList(
        @Body request: JsonRpcRequest<CommentListParams>,
    ): JsonRpcResponse<CommentListResult>

    @POST("api/v2")
    suspend fun commentListOwn(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.CommentListOwnParams>,
    ): JsonRpcResponse<CommentListResult>

    @POST("api/v2")
    suspend fun commentCreate(
        @Body request: JsonRpcRequest<CommentCreateParams>,
    ): JsonRpcResponse<CommentDto>

    @POST("api/v2")
    suspend fun commentReactList(
        @Body request: JsonRpcRequest<CommentReactListParams>,
    ): JsonRpcResponse<CommentReactListResult>

    @POST("api/v2")
    suspend fun commentReact(
        @Body request: JsonRpcRequest<CommentReactParams>,
    ): JsonRpcResponse<CommentReactResult>

    @POST("api/v2")
    suspend fun commentPin(
        @Body request: JsonRpcRequest<CommentPinParams>,
    ): JsonRpcResponse<CommentDto>

    @POST("api/v2")
    suspend fun commentEdit(
        @Body request: JsonRpcRequest<CommentEditParams>,
    ): JsonRpcResponse<CommentDto>

    @POST("api/v2")
    suspend fun commentAbandon(
        @Body request: JsonRpcRequest<CommentAbandonParams>,
    ): JsonRpcResponse<JsonObject>

    @POST("api/v2")
    suspend fun moderationBlock(
        @Body request: JsonRpcRequest<ModerationBlockParams>,
    ): JsonRpcResponse<JsonObject>

    @POST("api/v2")
    suspend fun moderationAddDelegate(
        @Body request: JsonRpcRequest<ModerationAddDelegateParams>,
    ): JsonRpcResponse<JsonObject>

    @POST("api/v2")
    suspend fun moderationRemoveDelegate(
        @Body request: JsonRpcRequest<ModerationRemoveDelegateParams>,
    ): JsonRpcResponse<JsonObject>

    @POST("api/v2")
    suspend fun settingGet(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.SettingGetParams>,
    ): JsonRpcResponse<com.odysee.app.core.network.dto.SettingResponse>

    @POST("api/v2")
    suspend fun settingUpdate(
        @Body request: JsonRpcRequest<JsonObject>,
    ): JsonRpcResponse<JsonObject>

    @POST("api/v2")
    suspend fun moderationBlockedList(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.ModerationBlockedListParams>,
    ): JsonRpcResponse<com.odysee.app.core.network.dto.ModerationBlockedListResult>

    @POST("api/v2")
    suspend fun moderationListDelegates(
        @Body request: JsonRpcRequest<com.odysee.app.core.network.dto.ModerationListDelegatesParams>,
    ): JsonRpcResponse<com.odysee.app.core.network.dto.ModerationListDelegatesResult>

    @POST("api/v2")
    suspend fun moderationUnblock(
        @Body request: JsonRpcRequest<ModerationBlockParams>,
    ): JsonRpcResponse<JsonObject>
}
