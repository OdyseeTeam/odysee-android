package com.odysee.app.core.network.jsonrpc

import kotlinx.serialization.Serializable

@Serializable
data class JsonRpcRequest<P>(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: P,
    val id: Long = 1L,
)

@Serializable
data class JsonRpcResponse<R>(
    val jsonrpc: String = "2.0",
    val result: R? = null,
    val error: JsonRpcError? = null,
    val id: Long = 1L,
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
)

class JsonRpcException(
    val code: Int,
    override val message: String,
) : RuntimeException("JSON-RPC $code: $message")

fun <R> JsonRpcResponse<R>.unwrap(): R {
    error?.let { throw JsonRpcException(it.code, it.message) }
    return result ?: throw JsonRpcException(-1, "Empty JSON-RPC result")
}
