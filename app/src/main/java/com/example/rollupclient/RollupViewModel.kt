package com.example.rollupclient


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rollupclient.data.remote.rpc.EthereumRpc
import com.example.rollupclient.data.remote.rpc.RollupRpc
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigInteger

class RollupViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RollupUiState())
    val uiState: StateFlow<RollupUiState> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null
    private var stateSyncJob: Job? = null
    private val appStartTime = System.currentTimeMillis()

    companion object {
        private const val TAG = "RollupViewModel"
    }

    init {
        Log.i(TAG, "🏗️ ViewModel initialized")
        refreshAll()
    }

    fun refreshAll() {
        Log.i(TAG, "🔄 Refreshing all networks...")
        refreshL1()
        refreshL2()
    }

    fun refreshL1() {
        Log.i(TAG, "🔄 Refreshing L1 network status...")
        viewModelScope.launch {
            _uiState.update { it.copy(isL1Syncing = true, l1Status = "Syncing...") }
            try {
                Log.d(TAG, "📡 Connecting to Ethereum L1 RPC...")
                val rpc = EthereumRpc()
                val block = rpc.getBlockNumber()
                Log.i(TAG, "✅ L1 sync successful: Block #$block")

                _uiState.update { currentState ->
                    currentState.copy(
                        l1BlockNumber = block,
                        l1Status = "Connected",
                        isL1Syncing = false,
                        networkError = null,
                        recentActivity = currentState.recentActivity + ActivityLog(
                            type = ActivityType.SYNC,
                            message = "L1 synced to block #$block",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                Log.i(TAG, "🎯 L1 status updated: Connected at block #$block")
            } catch (e: Exception) {
                Log.e(TAG, "❌ L1 sync failed: ${e.message}")
                _uiState.update { currentState ->
                    currentState.copy(
                        l1Status = "Disconnected",
                        isL1Syncing = false,
                        networkError = "L1: ${e.message}",
                        recentActivity = currentState.recentActivity + ActivityLog(
                            type = ActivityType.ERROR,
                            message = "L1 sync failed: ${e.message}",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                Log.w(TAG, "⚠️ L1 status updated: Disconnected")
            }
        }
    }

    fun refreshL2() {
        Log.i(TAG, "🔄 Refreshing L2 network status...")
        viewModelScope.launch {
            _uiState.update { it.copy(isL2Syncing = true, l2Status = "Syncing...") }
            try {
                Log.d(TAG, "📡 Connecting to Rollup L2 RPC...")
                val rpc = RollupRpc()
                val block = rpc.getBlockNumber()
                Log.i(TAG, "✅ L2 sync successful: Block #$block")

                _uiState.update { currentState ->
                    currentState.copy(
                        l2BlockNumber = block,
                        l2Status = "Connected",
                        isL2Syncing = false,
                        networkError = null,
                        recentActivity = currentState.recentActivity + ActivityLog(
                            type = ActivityType.SYNC,
                            message = "L2 synced to block #$block",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                Log.i(TAG, "🎯 L2 status updated: Connected at block #$block")
            } catch (e: Exception) {
                Log.e(TAG, "❌ L2 sync failed: ${e.message}")
                _uiState.update { currentState ->
                    currentState.copy(
                        l2Status = "Disconnected",
                        isL2Syncing = false,
                        networkError = "L2: ${e.message}",
                        recentActivity = currentState.recentActivity + ActivityLog(
                            type = ActivityType.ERROR,
                            message = "L2 sync failed: ${e.message}",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                Log.w(TAG, "⚠️ L2 status updated: Disconnected")
            }
        }
    }

    fun toggleMonitoring() {
        if (_uiState.value.isMonitoring) {
            Log.i(TAG, "🛑 User requested to stop monitoring")
            stopMonitoring()
        } else {
            Log.i(TAG, "▶️ User requested to start monitoring")
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = viewModelScope.launch {
            Log.i(TAG, "🚀 Starting rollup monitoring service...")
            _uiState.update { it.copy(isMonitoring = true) }

            // Initialize verification stats
            val startBlock = _uiState.value.l2BlockNumber ?: BigInteger.ZERO
            _uiState.update { it.copy(lastVerifiedBlock = startBlock) }

            Log.d(TAG, "📈 Starting monitoring from block #$startBlock")

            val verifier = SimpleRollupVerifier()
            var lastBlock = _uiState.value.lastVerifiedBlock ?: BigInteger.ZERO
            var consecutiveErrors = 0

            while (isActive) {
                try {
                    // Get latest block
                    val rpc = RollupRpc()
                    val latestBlock = rpc.getBlockNumber()

                    val newBlocks = latestBlock - lastBlock
                    if (newBlocks > BigInteger.ZERO) {
                        Log.i(TAG, "📊 Found $newBlocks new blocks to verify")
                    } else {
                        Log.d(
                            TAG,
                            "⏳ No new blocks found, current at #$lastBlock, latest is #$latestBlock"
                        )
                    }

                    // Update pending blocks
                    _uiState.update { currentState ->
                        currentState.copy(pendingBlocks = maxOf(0, newBlocks.toInt()))
                    }
                    Log.v(TAG, "📊 Pending blocks: ${maxOf(0, newBlocks.toInt())}")

                    // Verify new blocks
                    while (lastBlock < latestBlock && isActive) {
                        lastBlock = lastBlock + BigInteger.ONE

                        Log.d(TAG, "🔍 Verifying block #$lastBlock")
                        val startTime = System.currentTimeMillis()
                        val result = verifier.verifySingleBlock(lastBlock)
                        val endTime = System.currentTimeMillis()

                        val verificationTime = endTime - startTime
                        Log.d(TAG, "⏱️ Verification completed in ${verificationTime}ms")

                        if (result.isValid) {
                            consecutiveErrors = 0
                            Log.i(
                                TAG,
                                "✅ Block #$lastBlock verification PASSED (${verificationTime}ms)"
                            )
                        } else {
                            consecutiveErrors++
                            Log.w(
                                TAG,
                                "⚠️ Block #$lastBlock verification FAILED: ${result.errors.firstOrNull()}"
                            )
                            if (consecutiveErrors > 3) {
                                Log.e(
                                    TAG,
                                    "🚨 Too many consecutive errors ($consecutiveErrors), pausing monitoring"
                                )
                                delay(60000) // Wait 1 minute before continuing
                                consecutiveErrors = 0
                            }
                        }

                        // Calculate stats before updating state
                        val currentState = _uiState.value
                        val newVerified =
                            if (result.isValid) currentState.verifiedBlocks + 1 else currentState.verifiedBlocks
                        val newFailed =
                            if (!result.isValid) currentState.failedBlocks + 1 else currentState.failedBlocks
                        val totalBlocks = newVerified + newFailed
                        val newAvgTime = if (totalBlocks > 0) {
                            ((currentState.avgVerificationTimeMs * (totalBlocks - 1) + verificationTime) / totalBlocks).toInt()
                        } else verificationTime.toInt()
                        val newSuccessRate =
                            if (totalBlocks > 0) (newVerified * 100 / totalBlocks) else 0

                        _uiState.update { state ->
                            state.copy(
                                lastVerifiedBlock = lastBlock,
                                verifiedBlocks = newVerified,
                                failedBlocks = newFailed,
                                pendingBlocks = maxOf(0, state.pendingBlocks - 1),
                                lastVerification = "Block #$lastBlock: ${if (result.isValid) "✓ Valid" else "✗ Invalid"}",
                                lastVerificationValid = result.isValid,
                                avgVerificationTimeMs = newAvgTime,
                                successRate = newSuccessRate,
                                dataProcessedMB = state.dataProcessedMB + 1,
                                uptime = System.currentTimeMillis() - appStartTime,
                                recentActivity = state.recentActivity + ActivityLog(
                                    type = if (result.isValid) ActivityType.VERIFICATION else ActivityType.ERROR,
                                    message = "Block #$lastBlock ${if (result.isValid) "verified ✓" else "failed ✗"} (${verificationTime}ms)",
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }

                        Log.v(
                            TAG,
                            "📊 Stats updated: ${newVerified}✓ ${newFailed}✗ ${newAvgTime}ms avg, ${newSuccessRate}% success"
                        )
                        delay(2000) // Don't spam RPC
                    }

                    if (isActive) {
                        Log.d(TAG, "⏳ No new blocks to verify, checking again in 10s...")
                        delay(10000)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "💥 Monitoring error: ${e.javaClass.simpleName} - ${e.message}")
                    consecutiveErrors++

                    _uiState.update { currentState ->
                        currentState.copy(
                            recentActivity = currentState.recentActivity + ActivityLog(
                                type = ActivityType.ERROR,
                                message = "Monitoring error: ${e.message}",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }

                    if (consecutiveErrors > 5) {
                        Log.e(
                            TAG,
                            "🚨 Critical: Too many errors ($consecutiveErrors), stopping monitoring"
                        )
                        stopMonitoring()
                        break
                    }

                    Log.d(TAG, "⏳ Waiting 30s before retry...")
                    delay(30000)
                }
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        _uiState.update { it.copy(isMonitoring = false) }
        Log.i(TAG, "🛑 Monitoring service stopped")
    }

    suspend fun verifyLatestBlock() {
        Log.i(TAG, "🔍 Manual verification requested")
        _uiState.update { it.copy(isVerifying = true) }

        try {
            val rpc = RollupRpc()
            val latestBlock = rpc.getBlockNumber()
            Log.d(TAG, "📊 Latest L2 block: #$latestBlock")

            val verifier = SimpleRollupVerifier()

            val startTime = System.currentTimeMillis()
            val result = verifier.verifySingleBlock(latestBlock)
            val endTime = System.currentTimeMillis()

            val verificationTime = endTime - startTime

            if (result.isValid) {
                Log.i(TAG, "✅ Manual verification successful (${verificationTime}ms)")
            } else {
                Log.w(TAG, "⚠️ Manual verification failed: ${result.errors}")
            }

            // Calculate stats before updating state
            val currentState = _uiState.value
            val totalBlocks = currentState.verifiedBlocks + currentState.failedBlocks + 1
            val newAvgTime =
                ((currentState.avgVerificationTimeMs * (totalBlocks - 1) + verificationTime) / totalBlocks).toInt()

            _uiState.update { state ->
                state.copy(
                    lastVerification = "Block #$latestBlock: ${if (result.isValid) "✓ Valid" else "✗ Invalid"}",
                    lastVerificationValid = result.isValid,
                    avgVerificationTimeMs = newAvgTime,
                    isVerifying = false,
                    recentActivity = state.recentActivity + ActivityLog(
                        type = if (result.isValid) ActivityType.VERIFICATION else ActivityType.ERROR,
                        message = "Manual verification: Block #$latestBlock ${if (result.isValid) "✓" else "✗"} (${verificationTime}ms)",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            Log.i(TAG, "🎯 Manual verification completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Manual verification error: ${e.message}")
            _uiState.update { currentState ->
                currentState.copy(
                    lastVerification = "Verification failed: ${e.message}",
                    lastVerificationValid = false,
                    isVerifying = false,
                    recentActivity = currentState.recentActivity + ActivityLog(
                        type = ActivityType.ERROR,
                        message = "Manual verification failed: ${e.message}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun startStateSync() {
        Log.i(TAG, "🌳 Starting state trie sync...")
        stateSyncJob?.cancel()
        stateSyncJob = viewModelScope.launch {
            _uiState.update { it.copy(isStateSyncing = true, stateSyncProgress = 0f) }

            try {
                Log.d(
                    TAG,
                    "📥 Simulating state sync (this would fetch real state data from rollup)..."
                )
                for (progress in 0..100 step 5) {
                    if (!isActive) break

                    _uiState.update { currentState ->
                        val newActivity = if (progress % 20 == 0) {
                            Log.d(TAG, "📊 State sync progress: $progress%")
                            currentState.recentActivity + ActivityLog(
                                type = ActivityType.SYNC,
                                message = "State sync: $progress%",
                                timestamp = System.currentTimeMillis()
                            )
                        } else currentState.recentActivity

                        currentState.copy(
                            stateSyncProgress = progress / 100f,
                            stateTrieAccounts = progress * 10,
                            stateTrieSize = progress,
                            stateRoot = if (progress == 100) "0x1234...abcd" else null,
                            recentActivity = newActivity
                        )
                    }

                    delay(1000) // Simulate work
                }

                _uiState.update { it.copy(isStateSyncing = false) }
                Log.i(TAG, "✅ State trie sync completed")

            } catch (e: Exception) {
                Log.e(TAG, "❌ State sync failed: ${e.message}")
                _uiState.update { currentState ->
                    currentState.copy(
                        isStateSyncing = false,
                        recentActivity = currentState.recentActivity + ActivityLog(
                            type = ActivityType.ERROR,
                            message = "State sync failed: ${e.message}",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}

data class RollupUiState(
    val l1BlockNumber: BigInteger? = null,
    val l2BlockNumber: BigInteger? = null,
    val l1Status: String = "Disconnected",
    val l2Status: String = "Disconnected",
    val isL1Syncing: Boolean = false,
    val isL2Syncing: Boolean = false,
    val networkError: String? = null,

    // State Trie
    val stateTrieAccounts: Int = 0,
    val stateRoot: String? = null,
    val stateTrieSize: Int = 0,
    val stateSyncProgress: Float = 0f,
    val isStateSyncing: Boolean = false,

    // Verification
    val isMonitoring: Boolean = false,
    val isVerifying: Boolean = false,
    val verifiedBlocks: Int = 0,
    val pendingBlocks: Int = 0,
    val failedBlocks: Int = 0,
    val lastVerification: String? = null,
    val lastVerificationValid: Boolean? = null,
    val lastVerifiedBlock: BigInteger? = null,

    // Activity
    val recentActivity: List<ActivityLog> = emptyList(),

    // Stats
    val uptime: Long = 0,
    val dataProcessedMB: Int = 0,
    val avgVerificationTimeMs: Int = 0,
    val successRate: Int = 0
)

data class ActivityLog(
    val type: ActivityType,
    val message: String,
    val timestamp: Long
)

enum class ActivityType {
    VERIFICATION, SYNC, ERROR, INFO
}

enum class AlertType {
    SUCCESS, ERROR, WARNING, INFO
}

// Utility functions
fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "${diff / 1000}s ago"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}