package com.example.rollupclient.domain


import java.math.BigInteger

/**
 * Core data structures for our rollup client
 */

data class RollupBlock(
    val number: BigInteger,
    val hash: String,
    val parentHash: String,
    val timestamp: BigInteger,
    val l1BlockNumber: BigInteger,          // Which L1 block contains this rollup block
    val l1TxHash: String,                   // L1 transaction that posted this block
    val transactions: List<RollupTransaction>,
    val stateRoot: String,
    val transactionsRoot: String,
    val receiptsRoot: String,
    val gasUsed: BigInteger,
    val gasLimit: BigInteger
)

data class RollupTransaction(
    val hash: String,
    val from: String,
    val to: String?,
    val value: BigInteger,
    val data: String,
    val nonce: BigInteger,
    val gasLimit: BigInteger,
    val gasPrice: BigInteger,
    val chainId: BigInteger,
    val v: BigInteger,
    val r: String,
    val s: String,
    val transactionIndex: Int
)

data class RollupStateUpdate(
    val blockNumber: BigInteger,
    val blockHash: String,
    val accountUpdates: List<AccountUpdate>,
    val contractStorageUpdates: List<StorageUpdate>,
    val proof: MerkleProof
)

data class AccountUpdate(
    val address: String,
    val nonce: BigInteger,
    val balance: BigInteger,
    val storageRoot: String,
    val codeHash: String
)

data class StorageUpdate(
    val account: String,
    val slot: String,
    val value: String
)

data class MerkleProof(
    val leaf: String,
    val leafIndex: Int,
    val siblings: List<String>,
    val root: String
)

data class FraudProof(
    val disputedBlock: BigInteger,
    val disputedTransactionIndex: Int,
    val preState: RollupStateUpdate,
    val postState: RollupStateUpdate,
    val stateProof: MerkleProof,
    val transactionProof: MerkleProof,
    val signature: String,                  // Signed by challenger
    val timestamp: Long
)

data class StateSummary(
    val accounts: Int = 0,
    val totalBalance: BigInteger = BigInteger.ZERO,
    val latestStateRoot: String = "None",
    val trackedBlocks: Int = 0
)