package com.example.rollupclient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rollupclient.StatCard
import kotlinx.coroutines.launch
import java.math.BigInteger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RollupDashboard(
    viewModel: RollupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔗 Rollup Security Client") },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshAll() }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(
                        onClick = { /* TODO: Open settings */ }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        viewModel.toggleMonitoring()
                    }
                },
                containerColor = if (uiState.isMonitoring) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (uiState.isMonitoring) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            ) {
                Icon(
                    imageVector = if (uiState.isMonitoring) {
                        Icons.Outlined.Pause
                    } else {
                        Icons.Outlined.PlayArrow
                    },
                    contentDescription = if (uiState.isMonitoring) "Stop Monitoring" else "Start Monitoring"
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
            NetworkStatusCard(uiState, viewModel)
            StateTrieCard(uiState, viewModel)
            BlockVerificationCard(uiState, viewModel)
            RecentActivityCard(uiState)
            StatsCard(uiState)
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun NetworkStatusCard(
    uiState: RollupUiState,
    viewModel: RollupViewModel
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🌐 Network Status",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NetworkStatusItem(
                    label = "Ethereum L1",
                    block = uiState.l1BlockNumber,
                    status = uiState.l1Status,
                    isSyncing = uiState.isL1Syncing,
                    onClick = { viewModel.refreshL1() }
                )

                NetworkStatusItem(
                    label = "Rollup L2",
                    block = uiState.l2BlockNumber,
                    status = uiState.l2Status,
                    isSyncing = uiState.isL2Syncing,
                    onClick = { viewModel.refreshL2() }
                )
            }

            if (uiState.networkError != null) {
                AlertCard(
                    title = "Network Error",
                    message = uiState.networkError!!,
                    type = AlertType.ERROR
                )
            }
        }
    }
}

@Composable
private fun NetworkStatusItem(
    label: String,
    block: BigInteger?,
    status: String,
    isSyncing: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = block?.toString() ?: "—",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        AssistChip(
            onClick = onClick,
            label = { Text(status) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = when (status) {
                    "Connected" -> MaterialTheme.colorScheme.primaryContainer
                    "Disconnected" -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                labelColor = when (status) {
                    "Connected" -> MaterialTheme.colorScheme.onPrimaryContainer
                    "Disconnected" -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        )
    }
}

@Composable
private fun StateTrieCard(uiState: RollupUiState, viewModel: RollupViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌳 State Trie",
                    style = MaterialTheme.typography.titleMedium
                )

                AssistChip(
                    onClick = { /* TODO: Show state details */ },
                    label = { Text("Details") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Accounts",
                    value = uiState.stateTrieAccounts.toString()
                )
                StatItem(
                    label = "Root Hash",
                    value = uiState.stateRoot?.take(12) ?: "0x..."
                )
                StatItem(
                    label = "Size",
                    value = "${uiState.stateTrieSize} KB"
                )
            }

            if (uiState.stateSyncProgress > 0f) {
                LinearProgressIndicator(
                    progress = uiState.stateSyncProgress,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Syncing state: ${(uiState.stateSyncProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
            } else if (!uiState.isStateSyncing) {
                Button(
                    onClick = { viewModel.startStateSync() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start State Sync")
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun BlockVerificationCard(uiState: RollupUiState, viewModel: RollupViewModel) {
    val scope = rememberCoroutineScope()

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
            Text(
                text = "🔍 Block Verification",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VerificationStat(
                    label = "Verified",
                    value = uiState.verifiedBlocks,
                    color = MaterialTheme.colorScheme.primary
                )
                VerificationStat(
                    label = "Pending",
                    value = uiState.pendingBlocks,
                    color = MaterialTheme.colorScheme.secondary
                )
                VerificationStat(
                    label = "Failed",
                    value = uiState.failedBlocks,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (uiState.lastVerification != null) {
                AlertCard(
                    title = "Last Verification",
                    message = uiState.lastVerification!!,
                    type = if (uiState.lastVerificationValid == true) AlertType.SUCCESS else AlertType.ERROR
                )
            }

            Button(
                onClick = {
                    scope.launch {
                        viewModel.verifyLatestBlock()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isVerifying && uiState.l2BlockNumber != null
            ) {
                if (uiState.isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verifying...")
                } else {
                    Text("Verify Latest Block")
                }
            }
        }
    }
}

@Composable
private fun VerificationStat(label: String, value: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )
    }
}

@Composable
private fun AlertCard(title: String, message: String, type: AlertType) {
    val backgroundColor = when (type) {
        AlertType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        AlertType.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        AlertType.WARNING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        AlertType.INFO -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    val textColor = when (type) {
        AlertType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        AlertType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        AlertType.WARNING -> MaterialTheme.colorScheme.onSecondaryContainer
        AlertType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
    }
}

@Composable
private fun RecentActivityCard(uiState: RollupUiState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Recent Activity",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.recentActivity.isEmpty()) {
                Text(
                    text = "No recent activity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.recentActivity.take(5).forEach { activity ->
                    ActivityRow(activity)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.recentActivity.isNotEmpty()) {
                TextButton(
                    onClick = { /* TODO: Show all activity */ },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("View All (${uiState.recentActivity.size})")
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: ActivityLog) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (activity.type) {
                ActivityType.VERIFICATION -> Icons.Outlined.CheckCircle
                ActivityType.SYNC -> Icons.Outlined.Sync
                ActivityType.ERROR -> Icons.Outlined.Error
                ActivityType.INFO -> Icons.Outlined.Info
            },
            contentDescription = null,
            tint = when (activity.type) {
                ActivityType.VERIFICATION -> MaterialTheme.colorScheme.primary
                ActivityType.SYNC -> MaterialTheme.colorScheme.secondary
                ActivityType.ERROR -> MaterialTheme.colorScheme.error
                ActivityType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activity.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatTimeAgo(activity.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsCard(uiState: RollupUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📈 Statistics",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Simple grid layout
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Uptime",
                        value = formatDuration(uiState.uptime),
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Data",
                        value = "${uiState.dataProcessedMB} MB",
                        icon = Icons.Default.Storage,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Avg Time",
                        value = "${uiState.avgVerificationTimeMs} ms",
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Success Rate",
                        value = "${uiState.successRate}%",
                        icon = Icons.Default.Upcoming,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
