package com.example.rollupclient

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.math.BigInteger

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "RollupClient"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "🚀 App starting...")

        setContent {
            MaterialTheme {
                RollupDashboard()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDashboard() {
    var logs by remember { mutableStateOf("App started\n") }
    var ethBlock by remember { mutableStateOf<BigInteger?>(null) }
    var rollupBlock by remember { mutableStateOf<BigInteger?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun addLog(message: String) {
        logs = "${System.currentTimeMillis()}: $message\n$logs"
        Log.d("RollupClient", message)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔍 Rollup Client Debug",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Quick Test Buttons
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚡ Quick Tests",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                addLog("Testing Ethereum RPC...")
                                try {
                                    val rpc = EthereumRpc()
                                    val block = rpc.getBlockNumber()
                                    ethBlock = block
                                    addLog("✅ Ethereum: Block #$block")
                                } catch (e: Exception) {
                                    addLog("❌ Ethereum failed: ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test L1")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                addLog("Testing Rollup RPC...")
                                try {
                                    val rpc = RollupRpc()
                                    val block = rpc.getBlockNumber()
                                    rollupBlock = block
                                    addLog("✅ Rollup: Block #$block")
                                } catch (e: Exception) {
                                    addLog("❌ Rollup failed: ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test L2")
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            addLog("Testing full block fetch...")
                            try {
                                val rpc = RollupRpc()
                                val latest = rpc.getBlockNumber()
                                addLog("Latest rollup block: #$latest")

                                val block = rpc.getBlockByNumber(rpc.bigIntToHex(latest))
                                addLog("✅ Got full block:")
                                addLog("  Hash: ${block.hash.take(20)}...")
                                addLog("  Timestamp: ${block.timestamp}")
                                addLog("  L1 Block: ${block.l1BlockNumber ?: "Not found"}")
                                addLog("  Tx count: ${block.transactions.size}")
                                addLog("  State root: ${block.stateRoot.take(20)}...")
                            } catch (e: Exception) {
                                addLog("❌ Full block failed: ${e.message}")
                                e.printStackTrace()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Full Block Fetch")
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            addLog("Testing Ethereum block fetch...")
                            try {
                                val rpc = EthereumRpc()
                                val latest = rpc.getBlockNumber()
                                addLog("Latest Ethereum block: #$latest")

                                val block = rpc.getBlockByNumber(rpc.bigIntToHex(latest), false)
                                addLog("✅ Got Ethereum block:")
                                addLog("  Hash: ${block.hash.take(20)}...")
                                addLog("  Miner: ${block.miner.take(20)}...")
                                addLog("  Gas used: ${block.gasUsed}")
                                addLog("  Size: ${block.size} bytes")
                            } catch (e: Exception) {
                                addLog("❌ Ethereum block failed: ${e.message}")
                                e.printStackTrace()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Ethereum Block")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Verification Test
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔍 Verification Test",
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            addLog("Starting verification test...")
                            try {
                                val verifier = SimpleRollupVerifier()
                                val latest = RollupRpc().getBlockNumber()
                                addLog("Verifying rollup block #$latest")

                                val result = verifier.verifySingleBlock(latest)

                                addLog("=== VERIFICATION RESULT ===")
                                addLog("Rollup block: #${result.rollupBlock}")
                                addLog("L1 block: #${result.l1Block ?: "None"}")
                                addLog("Valid: ${result.isValid}")
                                if (result.errors.isNotEmpty()) {
                                    addLog("Errors:")
                                    result.errors.forEach { error ->
                                        addLog("  - $error")
                                    }
                                }
                                addLog("==========================")
                            } catch (e: Exception) {
                                addLog("❌ Verification failed: ${e.message}")
                                e.printStackTrace()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading && rollupBlock != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test Verification")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Display
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📊 Status",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Button(
                        onClick = { logs = "Cleared logs\n" },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Clear")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Log Display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Text(
                        text = logs,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }

        // Network Info
        if (ethBlock != null || rollupBlock != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (ethBlock != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ethereum", style = MaterialTheme.typography.labelSmall)
                            Text("#$ethBlock", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    if (rollupBlock != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rollup", style = MaterialTheme.typography.labelSmall)
                            Text("#$rollupBlock", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // Loading Indicator
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Processing...")
                }
            }
        }
    }
}