package com.example.rollupclient.domain

import java.math.BigInteger


/**
 * Manages rollup state locally - would eventually sync with actual state trie
 */
class StateManager {
    // In-memory state tracking
    private val accountBalances = mutableMapOf<String, BigInteger>()
    private val stateRoots = mutableMapOf<BigInteger, String>() // block number -> state root

    fun updateAccount(address: String, newBalance: BigInteger) {
        accountBalances[address] = newBalance
    }

    fun getAccountBalance(address: String): BigInteger {
        return accountBalances[address] ?: BigInteger.ZERO
    }

    fun saveStateRoot(blockNumber: BigInteger, stateRoot: String) {
        stateRoots[blockNumber] = stateRoot
    }

    fun getStateRoot(blockNumber: BigInteger): String? {
        return stateRoots[blockNumber]
    }

    fun getLatestStateRoot(): String? {
        return if (stateRoots.isEmpty()) null else stateRoots[stateRoots.keys.max()]
    }

    fun verifyStateTransition(
        oldRoot: String,
        newRoot: String,
        transactions: List<String>
    ): Boolean {
        // Simplified: In real implementation, this would verify Merkle proofs
        // For now, we just check they're different (transactions changed state)
        return oldRoot != newRoot
    }

    // For debugging - public accessors
    fun getAccountCount(): Int = accountBalances.size
    fun getTotalBalance(): BigInteger = accountBalances.values.fold(BigInteger.ZERO) { acc, value -> acc + value }

    fun getStateRootsCount(): Int = stateRoots.size

    // Additional helpers
    fun getTrackedBlockRange(): Pair<BigInteger?, BigInteger?> {
        return if (stateRoots.isEmpty()) {
            null to null
        } else {
            val min = stateRoots.keys.min()
            val max = stateRoots.keys.max()
            min to max
        }
    }

    fun clear() {
        accountBalances.clear()
        stateRoots.clear()
    }
}