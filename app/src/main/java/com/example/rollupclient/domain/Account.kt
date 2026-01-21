package com.example.rollupclient.domain


import java.math.BigInteger

/**
 * Ethereum Account State as per Yellow Paper
 */
data class AccountState(
    val nonce: BigInteger = BigInteger.ZERO,
    val balance: BigInteger = BigInteger.ZERO,
    val storageRoot: String = EMPTY_STORAGE_ROOT,
    val codeHash: String = EMPTY_CODE_HASH
) {
    companion object {
        // Empty tree root (Keccak256 of RLP null)
        const val EMPTY_STORAGE_ROOT = "0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"

        // Keccak256 of empty string
        const val EMPTY_CODE_HASH = "0xc5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470"
    }
}

/**
 * Full account with address
 */
data class Account(
    val address: String,
    val state: AccountState
)

/**
 * Storage slot for smart contracts
 */
data class StorageSlot(
    val address: String,
    val slot: String,
    val value: String
)