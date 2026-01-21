package com.example.rollupclient

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rollupclient.domain.AccountState
import java.math.BigInteger
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen() {
    val viewModel: RollupViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val summary = uiState.stateSummary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👤 Account Management") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // State Summary Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Trie State", style = MaterialTheme.typography.titleMedium)

                    StateItem("Accounts", summary.accounts.toString())
                    StateItem("Total Balance", "${summary.totalBalance} wei")
                    StateItem("State Root", summary.latestStateRoot.take(20) + "...")
                    StateItem("Tracked Blocks", summary.trackedBlocks.toString())
                }
            }

            // Test Accounts Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🧪 Test Actions", style = MaterialTheme.typography.titleMedium)

                    Button(
                        onClick = { viewModel.addTestAccount() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Test Account")
                    }

                    Button(
                        onClick = { /* Generate proof - implement later */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate State Proof")
                    }

                    Button(
                        onClick = {
                            // TODO: Add clear method to ViewModel
                            Log.d("AccountScreen", "Clear button pressed")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Test Data")
                    }
                }
            }

            // Account List Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("📝 Tracked Accounts", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (summary.accounts > 0) {
                        // Show actual account count
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${summary.accounts} account(s) in trie",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Total balance: ${summary.totalBalance} wei",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Latest state root:",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                summary.latestStateRoot.take(24) + "...",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Text(
                            "No accounts in state trie yet",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Debug Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("🔍 Debug Info", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Press 'Add Test Account' to add a test account to the Merkle Patricia Trie. " +
                                "This will update the state root hash and show the account count.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StateItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SampleAccountList() {
    val sampleAccounts = listOf(
        SampleAccount("0x742d35Cc6634C0532925a3b844Bc9e...", "100.0 ETH"),
        SampleAccount("0x53d284357ec70cE289D6D64134Df...", "50.5 ETH"),
        SampleAccount("0xC02aaA39b223FE8D0A0e5C4F27e...", "1000.0 ETH")
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sampleAccounts) { account ->
            AccountRow(account)
        }
    }
}

@Composable
private fun AccountRow(account: SampleAccount) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.AccountBalance,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.address,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    account.balance,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

data class SampleAccount(val address: String, val balance: String)