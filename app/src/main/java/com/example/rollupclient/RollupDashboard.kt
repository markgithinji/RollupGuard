package com.example.rollupclient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rollupclient.domain.RollupVerifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RollupDashboard() {
    val viewModel: RollupViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔐 Rollup Client") },
                actions = {
                    IconButton(onClick = { viewModel.refreshNetworkStatus() }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleMonitoring() },
                containerColor = if (uiState.isMonitoring) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Icon(
                    if (uiState.isMonitoring) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    if (uiState.isMonitoring) "Stop" else "Start"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Network Status Card
            NetworkStatusCard(uiState, viewModel)

            // State Summary Card
            StateSummaryCard(uiState)

            // Verification Stats Card
            VerificationStatsCard(uiState)

            // Last Verification Card
            uiState.lastVerification?.let { verification ->
                LastVerificationCard(verification)
            }
        }
    }
}

@Composable
private fun NetworkStatusCard(
    uiState: RollupUiState1,
    viewModel: RollupViewModel
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🌐 Network Status", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusChip(
                    label = "Ethereum L1",
                    status = uiState.l1Status,
                    block = uiState.l1BlockNumber?.toString() ?: "—",
                    isHealthy = uiState.networkHealth.l1Healthy
                )

                StatusChip(
                    label = "Rollup L2",
                    status = uiState.l2Status,
                    block = uiState.l2BlockNumber?.toString() ?: "—",
                    isHealthy = uiState.networkHealth.l2Healthy
                )
            }

            if (!uiState.networkHealth.allHealthy) {
                AlertCard(
                    message = "Network issues detected",
                    type = AlertType.WARNING
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    status: String,
    block: String,
    isHealthy: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(block, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        FilterChip(
            selected = isHealthy,
            onClick = {},
            label = { Text(status) },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = if (isHealthy) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        )
    }
}

@Composable
private fun StateSummaryCard(uiState: RollupUiState1) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Storage, contentDescription = null)
                Text("📊 State Summary", style = MaterialTheme.typography.titleMedium)
            }

            val summary = uiState.stateSummary

            // Check if we have any state data (trackedBlocks > 0)
            if (summary.trackedBlocks > 0) {
                // Display the state summary properties
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Accounts", style = MaterialTheme.typography.bodyMedium)
                    Text(summary.accounts.toString(), style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Balance", style = MaterialTheme.typography.bodyMedium)
                    Text("${summary.totalBalance} wei", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tracked Blocks", style = MaterialTheme.typography.bodyMedium)
                    Text(summary.trackedBlocks.toString(), style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Latest State Root", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        summary.latestStateRoot.take(12) + "...",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text("No state data yet", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun VerificationStatsCard(uiState: RollupUiState1) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🔍 Verification Stats", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Verified", uiState.verifiedBlocks.toString())
                StatItem("Failed", uiState.failedBlocks.toString())
                StatItem("Success", "${uiState.successRate}%")
            }

            if (uiState.pendingBlocks > 0) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "${uiState.pendingBlocks} blocks pending",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun LastVerificationCard(
    verification: RollupVerifier.VerificationResult
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📝 Last Verification", style = MaterialTheme.typography.titleMedium)

            Text("Block: #${verification.rollupBlock}")
            Text("Valid: ${if (verification.isValid) "✅" else "❌"}")
            Text("Time: ${verification.verificationTimeMs}ms")
            Text("Txs: ${verification.transactionsCount}")

            if (verification.errors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Notes:", style = MaterialTheme.typography.labelMedium)
                verification.errors.forEach { error ->
                    Text("• $error", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AlertCard(message: String, type: AlertType) {
    val backgroundColor = when (type) {
        AlertType.WARNING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "⚠️ $message",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
