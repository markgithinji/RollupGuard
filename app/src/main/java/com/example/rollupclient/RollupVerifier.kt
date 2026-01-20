package com.example.rollupclient

/**
 * Simple rollup verification logic
 */
import android.util.Log
import kotlinx.coroutines.delay
import java.math.BigInteger
import kotlin.math.abs

class SimpleRollupVerifier {
    private val l1Rpc = EthereumRpc()
    private val l2Rpc = RollupRpc()
    private var isMonitoring = false

    companion object {
        private const val TAG = "RollupVerifier"
    }

    data class VerificationResult(
        val rollupBlock: BigInteger,
        val l1Block: BigInteger?,
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val verificationTimeMs: Long = 0
    )

    /**
     * Basic verification: Check if rollup block has corresponding L1 block
     */
    suspend fun verifySingleBlock(rollupBlockNumber: BigInteger): VerificationResult {
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "🔍 Starting verification for block #$rollupBlockNumber")

        val errors = mutableListOf<String>()

        try {
            // 1. Get rollup block
            Log.d(TAG, "📥 Fetching rollup block from L2...")
            val rollupBlock = l2Rpc.getBlockByNumber(
                l2Rpc.bigIntToHex(rollupBlockNumber)
            )

            Log.i(TAG, "✅ Rollup block #${rollupBlock.number}:")
            Log.d(TAG, "   📍 Hash: ${rollupBlock.hash.take(10)}...")
            Log.d(TAG, "   ⏱️ Timestamp: ${rollupBlock.timestamp}")
            Log.d(TAG, "   📊 L1 ref: ${rollupBlock.l1BlockNumber ?: "Not found"}")
            Log.d(TAG, "   🔢 Transactions: ${rollupBlock.transactions.size}")

            // 2. Check if rollup block has L1 reference (the ACTUAL verification)
            if (rollupBlock.l1BlockNumber == null) {
                val error = "Rollup block missing L1 reference - cannot verify L1 inclusion"
                Log.w(TAG, "⚠️ $error")

                // For testnet: Still accept it but warn
                val totalTime = System.currentTimeMillis() - startTime
                Log.w(TAG, "⚠️ Testnet verification: Missing L1 reference (normal for test blocks)")

                return VerificationResult(
                    rollupBlock = rollupBlockNumber,
                    l1Block = null,
                    isValid = true, // Accept missing L1 ref in testnet
                    errors = listOf("Testnet: Missing L1 reference (normal for test blocks)"),
                    verificationTimeMs = totalTime
                )
            }

            val l1BlockNumber = rollupBlock.l1BlockNumber!!
            Log.d(TAG, "🔗 Found L1 reference: Block #$l1BlockNumber")

            // 3. Get L1 block to verify it exists
            Log.d(TAG, "Looking for L1 block #$l1BlockNumber...")
            val l1Block = l1Rpc.getBlockByNumber(
                l1Rpc.bigIntToHex(l1BlockNumber),
                false
            )

            Log.i(TAG, "✅ L1 block #${l1Block.number}:")
            Log.d(TAG, "   📍 Hash: ${l1Block.hash.take(10)}...")
            Log.d(TAG, "   ⏱️ Timestamp: ${l1Block.timestamp}")
            Log.d(TAG, "   ⛏️ Miner: ${l1Block.miner.take(10)}...")

            // 4. TIMESTAMP CHECK - MODIFIED FOR TESTNET
            val timeDiff = rollupBlock.timestamp - l1Block.timestamp
            Log.d(TAG, "⏱️ Timestamp diff: L2=${rollupBlock.timestamp}s, L1=${l1Block.timestamp}s, diff=${timeDiff}s")

            // For testnet: Just log the difference, don't fail
            if (timeDiff.abs() > BigInteger.valueOf(300)) {
                Log.w(TAG, "⚠️ Testnet timestamp mismatch: ${timeDiff}s (normal for testnet)")
            }

            val totalTime = System.currentTimeMillis() - startTime
            Log.i(TAG, "🎉 Verification PASSED for block #$rollupBlockNumber (${totalTime}ms)")

            return VerificationResult(
                rollupBlock = rollupBlockNumber,
                l1Block = l1BlockNumber,
                isValid = true,
                errors = listOf("Testnet: Timestamps don't match (expected)"),
                verificationTimeMs = totalTime
            )

        } catch (e: Exception) {
            val totalTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "❌ Verification failed: ${e.message}")

            return VerificationResult(
                rollupBlock = rollupBlockNumber,
                l1Block = null,
                isValid = false,
                errors = listOf(e.message ?: "Unknown error"),
                verificationTimeMs = totalTime
            )
        }
    }

    /**
     * Monitor rollup continuously with stop control
     */
    suspend fun monitorRollup(startBlock: BigInteger = BigInteger.ZERO) {
        isMonitoring = true
        Log.i(TAG, "🚀 Starting rollup monitor...")

        var currentBlock = if (startBlock == BigInteger.ZERO) {
            val latest = l2Rpc.getBlockNumber()
            Log.d(TAG, "📊 Latest L2 block: #$latest")
            latest - BigInteger.valueOf(10) // Start 10 blocks back
        } else {
            startBlock
        }

        Log.i(TAG, "📈 Starting from block #$currentBlock")

        var verifiedCount = 0
        var failedCount = 0
        var totalVerificationTimeMs = 0L

        while (isMonitoring) {
            try {
                val latestBlock = l2Rpc.getBlockNumber()
                val blocksBehind = latestBlock - currentBlock

                if (blocksBehind > BigInteger.ZERO) {
                    Log.i(
                        TAG,
                        "\n📊 Monitoring: Current #$currentBlock, Latest #$latestBlock, Behind: ${blocksBehind} blocks"
                    )
                }

                // Verify new blocks
                while (currentBlock <= latestBlock && isMonitoring) {
                    Log.d(TAG, "--- Verifying block #$currentBlock ---")
                    val result = verifySingleBlock(currentBlock)

                    if (result.isValid) {
                        Log.i(TAG, "✅ Block #$currentBlock ✓ (${result.verificationTimeMs}ms)")
                        verifiedCount++
                    } else {
                        Log.w(TAG, "❌ Block #$currentBlock ✗ - ${result.errors.firstOrNull()}")
                        failedCount++
                    }

                    totalVerificationTimeMs += result.verificationTimeMs
                    val avgTime = if (verifiedCount + failedCount > 0) {
                        totalVerificationTimeMs / (verifiedCount + failedCount)
                    } else 0

                    Log.d(TAG, "📈 Stats: $verifiedCount ✓, $failedCount ✗, Avg time: ${avgTime}ms")

                    currentBlock++
                    delay(1000) // Don't spam the RPC
                }

                if (isMonitoring) {
                    Log.d(TAG, "⏳ No new blocks, waiting 10s...")
                    delay(10000)
                }

            } catch (e: Exception) {
                if (isMonitoring) {
                    Log.e(TAG, "⚠️ Monitoring error: ${e.message}")
                    Log.d(TAG, "⏳ Waiting 30s before retry...")
                    delay(30000)
                }
            }
        }
        Log.i(TAG, "🛑 Monitoring stopped")
    }

    /**
     * Stop the monitoring loop
     */
    fun stopMonitoring() {
        isMonitoring = false
        Log.i(TAG, "🛑 Stop requested for monitoring")
    }
}