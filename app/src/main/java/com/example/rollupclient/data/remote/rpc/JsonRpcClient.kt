package com.example.rollupclient.data.remote.rpc

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class JsonRpcClient(
    private val endpoint: String,
    private val timeoutSeconds: Int = 30
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        ?: throw IllegalArgumentException("Could not create media type")

    companion object {
        private const val TAG = "JsonRpcClient"
    }

    suspend fun call(method: String, params: List<Any>): String {
        return withContext(Dispatchers.IO) {
            val requestId = (System.currentTimeMillis() % 10000).toInt()
            val paramsJson = JSONArray(params).toString()

            val requestBody = """
            {
                "jsonrpc": "2.0",
                "method": "$method",
                "params": $paramsJson,
                "id": $requestId
            }
            """.trimIndent()

            Log.i(TAG, "[Req#$requestId] $method → ${endpoint.take(40)}...")

            if (params.isNotEmpty()) {
                Log.d(TAG, "Params: ${params.take(2)}${if (params.size > 2) "..." else ""}")
            }

            val request = Request.Builder()
                .url(endpoint)
                .post(RequestBody.Companion.create(jsonMediaType, requestBody))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            val startTime = System.currentTimeMillis()

            try {
                val response: Response = client.newCall(request).execute()
                val duration = System.currentTimeMillis() - startTime

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No error body"
                    Log.e(
                        TAG,
                        "[${duration}ms] $method failed: ${response.code} ${response.message}"
                    )
                    Log.e(TAG, "Error: $errorBody")
                    throw RuntimeException("RPC ${response.code}: ${response.message}")
                }

                val responseBody = response.body?.string() ?: ""

                // Parse response
                val json = JSONObject(responseBody)

                if (json.has("error")) {
                    val error = json.getJSONObject("error")
                    Log.e(
                        TAG,
                        "[${duration}ms] $method error: ${error.getString("message")} (code: ${
                            error.optInt(
                                "code",
                                -1
                            )
                        })"
                    )
                    throw RuntimeException("RPC error: ${error.getString("message")}")
                }

                val result = json.opt("result")
                val resultStr = result?.toString() ?: "null"

                // Log based on method type
                when (method) {
                    "eth_blockNumber" -> {
                        Log.i(TAG, "[${duration}ms] Latest block: $resultStr")
                    }

                    "eth_getBlockByNumber" -> {
                        if (result is JSONObject) {
                            val blockNumber = result.optString("number", "unknown")
                            val txCount = result.optJSONArray("transactions")?.length() ?: 0
                            Log.i(
                                TAG,
                                "[${duration}ms] Block $blockNumber: ${txCount} txs, hash: ${
                                    result.optString(
                                        "hash",
                                        ""
                                    ).take(10)
                                }..."
                            )
                        }
                    }

                    else -> {
                        Log.i(
                            TAG,
                            "[${duration}ms] $method → ${resultStr.take(80)}${if (resultStr.length > 80) "..." else ""}"
                        )
                    }
                }

                return@withContext responseBody
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(
                    TAG,
                    "[${duration}ms] $method exception: ${e.javaClass.simpleName} - ${e.message}"
                )
                throw e
            }
        }
    }

    // Hex conversion methods
    fun hexToBigInt(hex: String): BigInteger {
        return try {
            val cleanHex = hex.removePrefix("0x").ifEmpty { "0" }
            val result = BigInteger(cleanHex, 16)
            Log.v(TAG, "Hex '$hex' → $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse hex: '$hex' - ${e.message}")
            BigInteger.ZERO
        }
    }

    fun bigIntToHex(value: BigInteger): String {
        val result = "0x${value.toString(16).lowercase()}"
        Log.v(TAG, "$value → hex '$result'")
        return result
    }
}