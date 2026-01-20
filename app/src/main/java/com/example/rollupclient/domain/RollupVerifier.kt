package com.example.rollupclient.domain

import android.util.Log
import com.example.rollupclient.absValue
import com.example.rollupclient.data.repository.RollupRepository
import com.example.rollupclient.toLongSafe
import java.math.BigInteger


class RollupVerifier {
    private val repository = RollupRepository()
    private val stateManager = StateManager()

    companion object {
        private const val TAG = "Verifier"
    }

    data class VerificationResult(
        val rollupBlock: BigInteger,
        val l1Block: BigInteger?,
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val verificationTimeMs: Long = 0,
        val stateRootValid: Boolean = false,
        val transactionsCount: Int = 0,
        val timestampOffset: Long = 0
    )


    suspend fun verifyBlockWithState(rollupBlockNumber: BigInteger): VerificationResult {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()

        try {
            Log.i(TAG, "🔍 Verification for block #$rollupBlockNumber")

            // 1. Get rollup block
            val rollupBlock = repository.getL2Block(rollupBlockNumber)
            Log.d(TAG, "Rollup block ${rollupBlock.number}: ${rollupBlock.transactions.size} txs")

            // 2. Get previous block for state root comparison
            val previousBlockNumber = rollupBlockNumber - BigInteger.ONE
            val previousStateRoot = stateManager.getStateRoot(previousBlockNumber)

            // 3. Check state root transition
            val stateRootValid = if (previousStateRoot != null) {
                stateManager.verifyStateTransition(
                    previousStateRoot,
                    rollupBlock.stateRoot,
                    rollupBlock.transactions
                )
            } else {
                Log.w(TAG, "No previous state root for comparison")
                true // First block in our view
            }

            if (!stateRootValid) {
                errors.add("State root transition invalid")
            }

            // 4. Save new state root
            stateManager.saveStateRoot(rollupBlockNumber, rollupBlock.stateRoot)

            // 5. Verify L1 inclusion
            val l1Block = rollupBlock.l1BlockNumber
            if (l1Block == null) {
                errors.add("Missing L1 reference - cannot verify L1 inclusion")
                Log.w(TAG, "Missing L1 reference (testnet normal)")
            } else {
                try {
                    val l1BlockData = repository.getL1Block(l1Block)
                    val timeDiff = (rollupBlock.timestamp - l1BlockData.timestamp)
                    val timeDiffLong = timeDiff.absValue().toLongSafe()

                    if (timeDiffLong > 300) {
                        errors.add("Large timestamp diff: ${timeDiffLong}s (testnet normal)")
                    }

                    Log.i(TAG, "✅ L1 block #$l1Block verified exists")
                } catch (e: Exception) {
                    errors.add("Failed to verify L1 block: ${e.message}")
                    Log.e(TAG, "L1 verification failed: ${e.message}")
                }
            }

            // 6. Check block finality
            val isFinalized = repository.isL2BlockFinalized(rollupBlockNumber)
            if (!isFinalized) {
                Log.i(TAG, "⚠️ Block not yet finalized (${rollupBlockNumber})")
            }

            val totalTime = System.currentTimeMillis() - startTime
            val isValid = errors.none { it.contains("failed") || it.contains("invalid") }

            val timestampOffsetLong = if (l1Block != null) {
                try {
                    val l1BlockData = repository.getL1Block(l1Block)
                    (rollupBlock.timestamp - l1BlockData.timestamp).toLongSafe()
                } catch (e: Exception) {
                    0L
                }
            } else 0L

            return VerificationResult(
                rollupBlock = rollupBlockNumber,
                l1Block = l1Block,
                isValid = isValid,
                errors = errors,
                verificationTimeMs = totalTime,
                stateRootValid = stateRootValid,
                transactionsCount = rollupBlock.transactions.size,
                timestampOffset = timestampOffsetLong
            )

        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "❌ verification failed: ${e.message}")

            return VerificationResult(
                rollupBlock = rollupBlockNumber,
                l1Block = null,
                isValid = false,
                errors = listOf("Critical error: ${e.message}"),
                verificationTimeMs = totalTime,
                stateRootValid = false,
                transactionsCount = 0,
                timestampOffset = 0
            )
        }
    }

    /**
     * Batch verification for efficiency
     */
    suspend fun verifyBlockRange(
        startBlock: BigInteger,
        endBlock: BigInteger
    ): List<VerificationResult> {
        Log.i(TAG, "📦 Batch verification from #$startBlock to #$endBlock")
        val results = mutableListOf<VerificationResult>()

        var currentBlock = startBlock
        while (currentBlock <= endBlock) {
            val result = verifyBlockWithState(currentBlock)
            results.add(result)

            if (!result.isValid) {
                Log.w(TAG, "❌ Block #$currentBlock failed, stopping batch")
                break
            }

            currentBlock++
        }

        return results
    }

    /**
     * Get state summary for UI - returns a proper data class instead of Map
     */
    fun getStateSummary(): StateSummary {
        return StateSummary(
            accounts = stateManager.getAccountCount(),
            totalBalance = stateManager.getTotalBalance(),
            latestStateRoot = stateManager.getLatestStateRoot() ?: "None",
            trackedBlocks = stateManager.getStateRootsCount()
        )
    }
}
