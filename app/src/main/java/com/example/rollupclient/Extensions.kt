package com.example.rollupclient

import java.math.BigInteger

// Extension functions for cleaner code
fun BigInteger.toHex(): String = "0x${this.toString(16).lowercase()}"

fun String.fromHex(): BigInteger =
    if (this.startsWith("0x")) {
        BigInteger(this.substring(2), 16)
    } else {
        BigInteger(this, 16)
    }

// Formatting utilities
fun formatBlockNumber(number: BigInteger?): String {
    return number?.toString() ?: "—"
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

// Helper for timestamp diff
fun BigInteger.absValue(): BigInteger = this.abs()
fun BigInteger.toLongSafe(): Long = if (this.bitLength() < 64) this.toLong() else Long.MAX_VALUE