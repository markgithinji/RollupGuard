package com.example.rollupclient.domain

import android.util.Log
import com.example.rollupclient.MerklePatriciaTrie
import com.example.rollupclient.RLP
import java.math.BigInteger


/**
 * Enhanced StateManager that uses Merkle Patricia Trie for actual state tracking
 */
class StateManager {
    private val stateTrie = MerklePatriciaTrie()
    private val accountCache = mutableMapOf<String, AccountState>()
    private val stateRootHistory = mutableMapOf<BigInteger, String>() // block -> state root

    companion object {
        // Convert hex address to bytes for trie key
        fun addressToKey(address: String): ByteArray {
            val cleanAddress = address.removePrefix("0x").lowercase()
            require(cleanAddress.length == 40) { "Invalid address length" }
            return hexStringToByteArray(cleanAddress)
        }

        // RLP encode account state as per Ethereum specification
        fun encodeAccount(account: AccountState): ByteArray {
            return RLP.encodeList(
                RLP.encodeBigInteger(account.nonce),
                RLP.encodeBigInteger(account.balance),
                RLP.encode(hexStringToByteArray(account.storageRoot.removePrefix("0x"))),
                RLP.encode(hexStringToByteArray(account.codeHash.removePrefix("0x")))
            )
        }

        fun decodeAccount(data: ByteArray): AccountState {
            val items = RLP.decodeToList(data)
            require(items.size == 4) { "Invalid account RLP" }

            return AccountState(
                nonce = if (items[0].isEmpty()) BigInteger.ZERO else BigInteger(1, items[0]),
                balance = if (items[1].isEmpty()) BigInteger.ZERO else BigInteger(1, items[1]),
                storageRoot = "0x" + bytesToHex(items[2]),
                codeHash = "0x" + bytesToHex(items[3])
            )
        }

        // Helper conversion functions
        private fun hexStringToByteArray(hex: String): ByteArray {
            val len = hex.length
            val data = ByteArray(len / 2)
            for (i in 0 until len step 2) {
                data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                        Character.digit(hex[i + 1], 16)).toByte()
            }
            return data
        }

        private fun bytesToHex(bytes: ByteArray): String {
            val hexChars = "0123456789abcdef"
            val result = StringBuilder(bytes.size * 2)
            for (byte in bytes) {
                val v = byte.toInt() and 0xFF
                result.append(hexChars[v shr 4])
                result.append(hexChars[v and 0x0F])
            }
            return result.toString()
        }
    }

    /**
     * Update or insert an account in the state trie
     */
    fun updateAccount(address: String, account: AccountState) {
        val addressKey = addressToKey(address)
        val accountData = encodeAccount(account)

        // Update trie
        stateTrie.put(addressKey, accountData)

        // Update cache
        accountCache[address.lowercase()] = account
    }

    /**
     * Get account state from trie
     */
    fun getAccount(address: String): AccountState? {
        val cached = accountCache[address.lowercase()]
        if (cached != null) return cached

        val addressKey = addressToKey(address)
        val accountData = stateTrie.get(addressKey)

        return if (accountData != null) {
            val account = decodeAccount(accountData)
            accountCache[address.lowercase()] = account
            account
        } else {
            null
        }
    }

    /**
     * Delete an account from state (suicide/selfdestruct)
     */
    fun deleteAccount(address: String) {
        val addressKey = addressToKey(address)
        stateTrie.delete(addressKey)
        accountCache.remove(address.lowercase())
    }

    /**
     * Get account balance (convenience method)
     */
    fun getAccountBalance(address: String): BigInteger {
        return getAccount(address)?.balance ?: BigInteger.ZERO
    }

    /**
     * Get current state root hash
     */
    fun getStateRoot(): String {
        return stateTrie.getRootHash()
    }

    /**
     * Save state root for a specific block
     */
    fun saveStateRoot(blockNumber: BigInteger, stateRoot: String? = null) {
        stateRootHistory[blockNumber] = stateRoot ?: getStateRoot()
    }

    /**
     * Get state root for a specific block
     */
    fun getStateRoot(blockNumber: BigInteger): String? {
        return stateRootHistory[blockNumber]
    }

    /**
     * Get the latest state root (most recent block)
     */
    fun getLatestStateRoot(): String? {
        return if (stateRootHistory.isEmpty()) {
            // Return current trie root if no history
            getStateRoot()
        } else {
            stateRootHistory[stateRootHistory.keys.max()]
        }
    }

    /**
     * Get number of tracked state roots (blocks)
     */
    fun getStateRootsCount(): Int = stateRootHistory.size

    /**
     * Get number of accounts in cache (approximate)
     */
    fun getAccountCount(): Int = accountCache.size

    /**
     * Calculate total balance of all tracked accounts
     */
    fun getTotalBalance(): BigInteger {
        return accountCache.values.fold(BigInteger.ZERO) { acc, account -> acc + account.balance }
    }

    /**
     * Verify state transition between two blocks (using block numbers)
     */
    fun verifyStateTransition(
        oldBlock: BigInteger,
        newBlock: BigInteger,
        transactions: List<String>
    ): Boolean {
        val oldRoot = getStateRoot(oldBlock)
        val newRoot = getStateRoot(newBlock)

        if (oldRoot == null || newRoot == null) {
            return false // Missing state data
        }

        // Simplified: Check that root changed (transactions affected state)
        // In reality, you'd verify each transaction's state transition
        return oldRoot != newRoot
    }

    fun verifyStateTransition(
        oldRoot: String,
        newRoot: String,
        transactions: List<String>
    ): Boolean {
        // Simplified: Just check if roots are different
        // In reality, you'd need to verify the transactions actually cause this change
        return oldRoot != newRoot
    }

    /**
     * Generate Merkle proof for an account at specific block
     */
    fun generateAccountProof(address: String, blockNumber: BigInteger): List<ByteArray>? {
        // TODO: Implement proof generation using trie state at specific block
        // This requires storing trie nodes per block
        return null
    }

    /**
     * Verify account proof against state root
     */
    fun verifyAccountProof(
        stateRoot: String,
        address: String,
        account: AccountState?,
        proof: List<ByteArray>
    ): Boolean {
        val addressKey = addressToKey(address)
        val accountData = account?.let { encodeAccount(it) } ?: byteArrayOf()

        return stateTrie.verifyProof(stateRoot, addressKey, accountData, proof)
    }
}