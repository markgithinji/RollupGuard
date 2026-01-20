package com.example.rollupclient.data.remote.rpc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigInteger

/**
 * COMPLETE Ethereum RPC client with all needed methods
 */
class EthereumRpc(
    private val rpcUrl: String = "https://ethereum-sepolia.publicnode.com"
) {
    val client = JsonRpcClient(rpcUrl)

    // Add hex helper methods
    fun bigIntToHex(value: BigInteger): String = client.bigIntToHex(value)
    fun hexToBigInt(hex: String): BigInteger = client.hexToBigInt(hex)

    // Update EthereumRpc.Block data class:
    data class Block(
        val number: BigInteger,
        val hash: String,
        val parentHash: String,
        val timestamp: BigInteger,
        val transactionsRoot: String,
        val stateRoot: String,
        val receiptsRoot: String,
        val miner: String,
        val difficulty: BigInteger = BigInteger.ZERO,
        val totalDifficulty: BigInteger? = null,
        val size: BigInteger = BigInteger.ZERO,
        val gasUsed: BigInteger = BigInteger.ZERO,
        val gasLimit: BigInteger = BigInteger.ZERO,
        val baseFeePerGas: BigInteger? = null
    )

    /**
     * Get block by number
     */
    suspend fun getBlockByNumber(blockNumber: String, fullTransactions: Boolean = false): Block {
        return withContext(Dispatchers.IO) {
            val response =
                client.call("eth_getBlockByNumber", listOf(blockNumber, fullTransactions))
            val json = JSONObject(response)

            if (json.has("error")) {
                val error = json.getJSONObject("error")
                throw RuntimeException("RPC error ${error.getInt("code")}: ${error.getString("message")}")
            }

            val result = json.getJSONObject("result")
            return@withContext parseBlock(result)
        }
    }

    /**
     * Get the latest block number
     */
    suspend fun getBlockNumber(): BigInteger {
        return withContext(Dispatchers.IO) {
            val response = client.call("eth_blockNumber", emptyList())
            val json = JSONObject(response)

            if (json.has("error")) {
                val error = json.getJSONObject("error")
                throw RuntimeException("RPC error ${error.getInt("code")}: ${error.getString("message")}")
            }

            val hexNumber = json.getString("result")
            return@withContext client.hexToBigInt(hexNumber)
        }
    }

    /**
     * Get transaction count for a block
     */
    suspend fun getBlockTransactionCount(blockNumber: String): Int {
        val response = client.call("eth_getBlockTransactionCountByNumber", listOf(blockNumber))
        val json = JSONObject(response)
        val hexCount = json.getString("result")
        return client.hexToBigInt(hexCount).toInt()
    }

    /**
     * Get transaction by hash
     */
    suspend fun getTransactionByHash(txHash: String): JSONObject {
        val response = client.call("eth_getTransactionByHash", listOf(txHash))
        val json = JSONObject(response)

        if (json.has("error")) {
            val error = json.getJSONObject("error")
            throw RuntimeException("RPC error: ${error.getString("message")}")
        }

        return json.getJSONObject("result")
    }

    /**
     * Get transaction receipt
     */
    suspend fun getTransactionReceipt(txHash: String): JSONObject {
        val response = client.call("eth_getTransactionReceipt", listOf(txHash))
        val json = JSONObject(response)

        if (json.has("error")) {
            val error = json.getJSONObject("error")
            throw RuntimeException("RPC error: ${error.getString("message")}")
        }

        return json.getJSONObject("result")
    }

    /**
     * Wait for transaction confirmation
     */
    suspend fun waitForTransactionConfirmations(
        txHash: String,
        requiredConfirmations: Int = 8,
        checkIntervalMs: Long = 2000
    ): Boolean {
        var receipt: JSONObject? = null
        var attempts = 0
        val maxAttempts = 60 // 2 minutes max

        while (receipt == null && attempts < maxAttempts) {
            try {
                receipt = getTransactionReceipt(txHash)
            } catch (e: Exception) {
                // Ignore and retry
            }

            if (receipt == null) {
                delay(checkIntervalMs)
                attempts++
            }
        }

        if (receipt == null) return false

        // Get current block
        val currentBlock = getBlockNumber()
        val txBlockNumber = client.hexToBigInt(receipt.getString("blockNumber"))

        // Check confirmations
        val confirmations = currentBlock - txBlockNumber
        return confirmations >= BigInteger.valueOf(requiredConfirmations.toLong())
    }

    /**
     * Get chain ID
     */
    suspend fun getChainId(): BigInteger {
        val response = client.call("eth_chainId", emptyList())
        val json = JSONObject(response)
        val hexChainId = json.getString("result")
        return client.hexToBigInt(hexChainId)
    }

    /**
     * Get client version
     */
    suspend fun getClientVersion(): String {
        val response = client.call("web3_clientVersion", emptyList())
        val json = JSONObject(response)
        return json.getString("result")
    }

    private fun parseBlock(json: JSONObject): Block {
        return Block(
            number = client.hexToBigInt(json.getString("number")),
            hash = json.getString("hash"),
            parentHash = json.getString("parentHash"),
            timestamp = client.hexToBigInt(json.getString("timestamp")),
            transactionsRoot = json.getString("transactionsRoot"),
            stateRoot = json.getString("stateRoot"),
            receiptsRoot = json.getString("receiptsRoot"),
            miner = json.getString("miner"),
            difficulty = client.hexToBigInt(json.getString("difficulty")),
            totalDifficulty = if (json.has("totalDifficulty")) {
                client.hexToBigInt(json.getString("totalDifficulty"))
            } else {
                null // Accept that it's null for post-Merge blocks
            },
            size = client.hexToBigInt(json.getString("size")),
            gasUsed = client.hexToBigInt(json.getString("gasUsed")),
            gasLimit = client.hexToBigInt(json.getString("gasLimit")),
            baseFeePerGas = if (json.has("baseFeePerGas")) {
                client.hexToBigInt(json.getString("baseFeePerGas"))
            } else null
        )
    }
}