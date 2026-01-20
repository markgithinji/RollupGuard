package com.example.rollupclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigInteger


class RollupRpc(
    private val chain: RollupChain = RollupChain.OPTIMISM_SEPOLIA
) {
    val client = JsonRpcClient(chain.rpcUrl)

    fun bigIntToHex(value: BigInteger): String = client.bigIntToHex(value)
    fun hexToBigInt(hex: String): BigInteger = client.hexToBigInt(hex)

    enum class RollupChain(
        val rpcUrl: String,
        val chainId: Int,
        val displayName: String
    ) {
        OPTIMISM_SEPOLIA(
            rpcUrl = "https://sepolia.optimism.io",
            chainId = 11155420,
            displayName = "Optimism Sepolia"
        ),

        ARBITRUM_SEPOLIA(
            rpcUrl = "https://sepolia-rollup.arbitrum.io/rpc",
            chainId = 421614,
            displayName = "Arbitrum Sepolia"
        )
    }

    data class RollupBlock(
        val number: BigInteger,
        val hash: String,
        val parentHash: String,
        val timestamp: BigInteger,
        val l1BlockNumber: BigInteger? = null,
        val transactions: List<String>,
        val stateRoot: String,
        val transactionsRoot: String,
        val receiptsRoot: String
    )

    /**
     * Get rollup block by number
     */
    suspend fun getBlockByNumber(blockNumber: String): RollupBlock {
        return withContext(Dispatchers.IO) {
            val response = client.call("eth_getBlockByNumber", listOf(blockNumber, false))
            val json = JSONObject(response)

            if (json.has("error")) {
                throw RuntimeException("Rollup RPC error: ${json.getJSONObject("error")}")
            }

            val result = json.getJSONObject("result")
            return@withContext parseRollupBlock(result)
        }
    }

    /**
     * Get latest rollup block number
     */
    suspend fun getBlockNumber(): BigInteger {
        return withContext(Dispatchers.IO) {
            val response = client.call("eth_blockNumber", emptyList())
            val json = JSONObject(response)
            val hexNumber = json.getString("result")
            return@withContext client.hexToBigInt(hexNumber)
        }
    }

    /**
     * Simplified finality check
     */
    suspend fun isBlockFinalized(blockNumber: BigInteger): Boolean {
        // For testnet, assume blocks older than 50 blocks are finalized enough
        val currentBlock = getBlockNumber()
        return (currentBlock - blockNumber) > BigInteger.valueOf(50)
    }

    /**
     * Get transaction receipt from rollup
     */
    suspend fun getTransactionReceipt(txHash: String): JSONObject {
        return withContext(Dispatchers.IO) {
            val response = client.call("eth_getTransactionReceipt", listOf(txHash))
            val json = JSONObject(response)

            if (json.has("error")) {
                throw RuntimeException("Failed to get receipt: ${json.getJSONObject("error")}")
            }

            return@withContext json.getJSONObject("result")
        }
    }

    private fun parseRollupBlock(json: JSONObject): RollupBlock {
        // Parse transaction hashes
        val transactionsArray = json.getJSONArray("transactions")
        val transactions = mutableListOf<String>()
        for (i in 0 until transactionsArray.length()) {
            transactions.add(transactionsArray.getString(i))
        }

        return RollupBlock(
            number = client.hexToBigInt(json.getString("number")),
            hash = json.getString("hash"),
            parentHash = json.getString("parentHash"),
            timestamp = client.hexToBigInt(json.getString("timestamp")),
            l1BlockNumber = if (json.has("l1BlockNumber")) {
                client.hexToBigInt(json.getString("l1BlockNumber"))
            } else null,
            transactions = transactions,
            stateRoot = json.getString("stateRoot"),
            transactionsRoot = json.getString("transactionsRoot"),
            receiptsRoot = json.getString("receiptsRoot")
        )
    }
}