package com.odysee.app.core.network.jsonrpc

import java.util.concurrent.atomic.AtomicLong
import kotlinx.serialization.Serializable

private val rpcIdCounter = AtomicLong(0)

/** Process-wide monotonic JSON-RPC request id. Spec recommends ids be unique per request. */
internal fun nextJsonRpcId(): Long = rpcIdCounter.incrementAndGet()

@Serializable
data class JsonRpcRequest<P>(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: P,
    val id: Long = nextJsonRpcId(),
)

@Serializable
data class JsonRpcResponse<R>(
    val jsonrpc: String = "2.0",
    val result: R? = null,
    val error: JsonRpcError? = null,
    val id: Long = 0L,
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
