package com.example.rollupclient.data.repository

import com.example.rollupclient.data.remote.rpc.EthereumRpc
import com.example.rollupclient.data.remote.rpc.RollupRpc
import java.math.BigInteger

class RollupRepository {
    private val ethRpc = EthereumRpc()
    private val rollupRpc = RollupRpc()

    // Cache for performance
    private var cachedL1Block: BigInteger? = null
    private var cachedL2Block: BigInteger? = null

    suspend fun getL1BlockNumber(): BigInteger {
        return try {
            val block = ethRpc.getBlockNumber()
            cachedL1Block = block
            block
        } catch (e: Exception) {
            throw RollupRepositoryException("Failed to get L1 block: ${e.message}")
        }
    }

    suspend fun getL2BlockNumber(): BigInteger {
        return try {
            val block = rollupRpc.getBlockNumber()
            cachedL2Block = block
            block
        } catch (e: Exception) {
            throw RollupRepositoryException("Failed to get L2 block: ${e.message}")
        }
    }

    suspend fun getL2Block(blockNumber: BigInteger): RollupRpc.RollupBlock {
        return try {
            rollupRpc.getBlockByNumber(rollupRpc.bigIntToHex(blockNumber))
        } catch (e: Exception) {
            throw RollupRepositoryException("Failed to get L2 block $blockNumber: ${e.message}")
        }
    }

    suspend fun getL1Block(blockNumber: BigInteger): EthereumRpc.Block {
        return try {
            ethRpc.getBlockByNumber(ethRpc.bigIntToHex(blockNumber), false)
        } catch (e: Exception) {
            throw RollupRepositoryException("Failed to get L1 block $blockNumber: ${e.message}")
        }
    }

    suspend fun isL2BlockFinalized(blockNumber: BigInteger): Boolean {
        return try {
            rollupRpc.isBlockFinalized(blockNumber)
        } catch (e: Exception) {
            throw RollupRepositoryException("Failed to check finality: ${e.message}")
        }
    }

    // Health check
    suspend fun checkL1Health(): Boolean {
        return try {
            ethRpc.getBlockNumber()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkL2Health(): Boolean {
        return try {
            rollupRpc.getBlockNumber()
            true
        } catch (e: Exception) {
            false
        }
    }
}

class RollupRepositoryException(message: String) : Exception(message)