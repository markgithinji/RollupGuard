package com.example.rollupclient


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rollupclient.data.repository.RollupRepository
import com.example.rollupclient.domain.RollupVerifier
import com.example.rollupclient.domain.StateSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigInteger


class RollupViewModel : ViewModel() {
    private val repository = RollupRepository()
    private val verifier = RollupVerifier()

    private val _uiState = MutableStateFlow(RollupUiState1())
    val uiState: StateFlow<RollupUiState1> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null
    private val appStartTime = System.currentTimeMillis()

    init {
        refreshNetworkStatus()
    }

    fun refreshNetworkStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val l1Job = launch { checkL1Connection() }
            val l2Job = launch { checkL2Connection() }

            l1Job.join()
            l2Job.join()

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun checkL1Connection() {
        _uiState.update { it.copy(l1Status = "Checking...") }

        try {
            val block = repository.getL1BlockNumber()
            _uiState.update { current ->
                current.copy(
                    l1BlockNumber = block,
                    l1Status = "Connected",
                    networkHealth = current.networkHealth.copy(l1Healthy = true)
                )
            }
        } catch (e: Exception) {
            _uiState.update { current ->
                current.copy(
                    l1Status = "Disconnected",
                    networkHealth = current.networkHealth.copy(l1Healthy = false)
                )
            }
        }
    }

    private suspend fun checkL2Connection() {
        _uiState.update { it.copy(l2Status = "Checking...") }

        try {
            val block = repository.getL2BlockNumber()
            _uiState.update { current ->
                current.copy(
                    l2BlockNumber = block,
                    l2Status = "Connected",
                    networkHealth = current.networkHealth.copy(l2Healthy = true)
                )
            }
        } catch (e: Exception) {
            _uiState.update { current ->
                current.copy(
                    l2Status = "Disconnected",
                    networkHealth = current.networkHealth.copy(l2Healthy = false)
                )
            }
        }
    }

    fun toggleMonitoring() {
        if (_uiState.value.isMonitoring) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = viewModelScope.launch {
            _uiState.update { it.copy(isMonitoring = true) }

            var lastVerifiedBlock = _uiState.value.l2BlockNumber ?: BigInteger.ZERO
            var consecutiveErrors = 0

            while (true) {
                try {
                    val latestBlock = repository.getL2BlockNumber()

                    if (latestBlock > lastVerifiedBlock) {
                        val blocksToVerify = (latestBlock - lastVerifiedBlock).toInt()
                        _uiState.update { it.copy(pendingBlocks = blocksToVerify) }

                        // Verify new blocks
                        while (lastVerifiedBlock < latestBlock) {
                            lastVerifiedBlock++

                            val result = verifier.verifyBlockWithState(lastVerifiedBlock)

                            // Get state summary directly
                            val stateSummary = verifier.getStateSummary()

                            // Update stats
                            _uiState.update { current ->
                                val verified = if (result.isValid) current.verifiedBlocks + 1 else current.verifiedBlocks
                                val failed = if (!result.isValid) current.failedBlocks + 1 else current.failedBlocks
                                val total = verified + failed

                                current.copy(
                                    lastVerifiedBlock = lastVerifiedBlock,
                                    verifiedBlocks = verified,
                                    failedBlocks = failed,
                                    pendingBlocks = maxOf(0, current.pendingBlocks - 1),
                                    successRate = if (total > 0) (verified * 100 / total) else 0,
                                    stateSummary = stateSummary,
                                    recentVerifications = current.recentVerifications + result
                                )
                            }

                            delay(1000) // Rate limiting
                        }
                    }

                    delay(5000) // Check every 5 seconds
                } catch (e: Exception) {
                    consecutiveErrors++
                    if (consecutiveErrors > 5) {
                        _uiState.update { it.copy(isMonitoring = false) }
                        break
                    }
                    delay(10000) // Wait longer on error
                }
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        _uiState.update { it.copy(isMonitoring = false) }
    }

    suspend fun verifySingleBlock(blockNumber: BigInteger? = null) {
        val targetBlock = blockNumber ?: repository.getL2BlockNumber()

        _uiState.update { it.copy(isVerifying = true) }

        try {
            val result = verifier.verifyBlockWithState(targetBlock)

            // Get state summary directly
            val stateSummary = verifier.getStateSummary()

            _uiState.update { current ->
                current.copy(
                    lastVerification = result,
                    stateSummary = stateSummary,
                    isVerifying = false
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isVerifying = false) }
        }
    }
}

data class RollupUiState1(
    // Network status
    val l1BlockNumber: BigInteger? = null,
    val l2BlockNumber: BigInteger? = null,
    val l1Status: String = "Disconnected",
    val l2Status: String = "Disconnected",
    val isRefreshing: Boolean = false,

    // Network health
    val networkHealth: NetworkHealth = NetworkHealth(),

    // Verification
    val isMonitoring: Boolean = false,
    val isVerifying: Boolean = false,
    val verifiedBlocks: Int = 0,
    val pendingBlocks: Int = 0,
    val failedBlocks: Int = 0,
    val successRate: Int = 0,

    // State
    val stateSummary: StateSummary = StateSummary(),
    val lastVerifiedBlock: BigInteger? = null,
    val lastVerification: RollupVerifier.VerificationResult? = null,
    val recentVerifications: List<RollupVerifier.VerificationResult> = emptyList(),

    // Stats
    val uptimeMs: Long = 0
)

data class NetworkHealth(
    val l1Healthy: Boolean = false,
    val l2Healthy: Boolean = false,
    val lastHealthCheck: Long = 0
) {
    val allHealthy: Boolean get() = l1Healthy && l2Healthy
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