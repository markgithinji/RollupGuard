package com.example.rollupclient

import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.experimental.xor

/**
 * Ethereum's Modified Merkle Patricia Trie (MPT) for state storage
 * Follows Ethereum Yellow Paper specification
 */
class MerklePatriciaTrie {

    sealed class Node {
        data class Branch(
            val children: Array<Node?>,  // 16 children for hex nibbles (0-15)
            val value: ByteArray? = null // Optional value at this node
        ) : Node() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Branch
                if (!children.contentEquals(other.children)) return false
                if (value != null && other.value != null) {
                    if (!value.contentEquals(other.value)) return false
                } else if (value != null || other.value != null) {
                    return false
                }
                return true
            }

            override fun hashCode(): Int {
                var result = children.contentHashCode()
                result = 31 * result + (value?.contentHashCode() ?: 0)
                return result
            }
        }

        data class Extension(
            val sharedNibbles: ByteArray,
            val next: Node
        ) : Node() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Extension
                if (!sharedNibbles.contentEquals(other.sharedNibbles)) return false
                if (next != other.next) return false
                return true
            }

            override fun hashCode(): Int {
                var result = sharedNibbles.contentHashCode()
                result = 31 * result + next.hashCode()
                return result
            }
        }

        data class Leaf(
            val remainingNibbles: ByteArray,
            val value: ByteArray
        ) : Node() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Leaf
                if (!remainingNibbles.contentEquals(other.remainingNibbles)) return false
                if (!value.contentEquals(other.value)) return false
                return true
            }

            override fun hashCode(): Int {
                var result = remainingNibbles.contentHashCode()
                result = 31 * result + value.contentHashCode()
                return result
            }
        }
    }

    private var root: Node? = null
    private val nodes = mutableMapOf<String, Node>() // Node hash -> Node cache

    // Keccak-256 hash (simplified - use proper implementation in production)
    private fun keccak256(data: ByteArray): ByteArray {
        // Simplified - in production use proper Keccak
        return data.copyOf() // TODO: Replace with real Keccak
    }

    private fun encodeNode(node: Node): ByteArray {
        return when (node) {
            is Node.Branch -> encodeBranch(node)
            is Node.Extension -> encodeExtension(node)
            is Node.Leaf -> encodeLeaf(node)
        }
    }

    private fun encodeBranch(branch: Node.Branch): ByteArray {
        val buffer = ByteBuffer.allocate(17 * 32 + 1) // 16 children + optional value
        branch.children.forEach { child ->
            if (child != null) {
                buffer.put(hashNode(child))
            } else {
                buffer.put(ByteArray(32)) // Empty
            }
        }
        branch.value?.let { buffer.put(it) }
        return buffer.array()
    }

    private fun encodeExtension(extension: Node.Extension): ByteArray {
        val buffer = ByteBuffer.allocate(extension.sharedNibbles.size + 32)
        buffer.put(extension.sharedNibbles)
        buffer.put(hashNode(extension.next))
        return buffer.array()
    }

    private fun encodeLeaf(leaf: Node.Leaf): ByteArray {
        val buffer = ByteBuffer.allocate(leaf.remainingNibbles.size + leaf.value.size)
        buffer.put(leaf.remainingNibbles)
        buffer.put(leaf.value)
        return buffer.array()
    }

    private fun hashNode(node: Node): ByteArray {
        val encoded = encodeNode(node)
        return keccak256(encoded)
    }

    fun getRootHash(): String {
        return if (root != null) {
            bytesToHex(hashNode(root!!))
        } else {
            "0x" + "0".repeat(64) // Empty tree hash
        }
    }

    /**
     * Insert or update a key-value pair in the trie
     */
    fun put(key: ByteArray, value: ByteArray) {
        val nibbles = bytesToNibbles(key)
        root = insert(root, nibbles, 0, value)
    }

    /**
     * Get value for a key
     */
    fun get(key: ByteArray): ByteArray? {
        val nibbles = bytesToNibbles(key)
        return retrieve(root, nibbles, 0)
    }

    /**
     * Delete a key from the trie
     */
    fun delete(key: ByteArray) {
        val nibbles = bytesToNibbles(key)
        root = remove(root, nibbles, 0)
    }

    /**
     * Verify a Merkle proof
     */
    fun verifyProof(rootHash: String, key: ByteArray, value: ByteArray, proof: List<ByteArray>): Boolean {
        val targetHash = keccak256(value)
        val nibbles = bytesToNibbles(key)

        var currentHash = hexToBytes(rootHash.removePrefix("0x"))

        for (i in proof.indices) {
            val node = proof[i]
            val nodeHash = keccak256(node)

            if (!nodeHash.contentEquals(currentHash)) {
                return false
            }

            // TODO: Verify node structure matches key path

            // Move to next node in proof
            currentHash = if (i < proof.size - 1) {
                // Extract child hash from node
                val childIndex = nibbles[i / 2] // Simplified
                val offset = if (childIndex.toInt() < 8) 0 else 1
                val start = (nibbles[i] * 32 + offset).toInt()
                node.copyOfRange(start, start + 32)
            } else {
                targetHash
            }
        }

        return currentHash.contentEquals(targetHash)
    }

    private fun insert(node: Node?, nibbles: ByteArray, depth: Int, value: ByteArray): Node {
        if (node == null) {
            // Create new leaf
            val remainingNibbles = nibbles.copyOfRange(depth, nibbles.size)
            return Node.Leaf(remainingNibbles, value)
        }

        return when (node) {
            is Node.Leaf -> insertIntoLeaf(node, nibbles, depth, value)
            is Node.Extension -> insertIntoExtension(node, nibbles, depth, value)
            is Node.Branch -> insertIntoBranch(node, nibbles, depth, value)
        }
    }

    private fun insertIntoLeaf(leaf: Node.Leaf, nibbles: ByteArray, depth: Int, value: ByteArray): Node {
        // Find common prefix
        val common = commonPrefix(leaf.remainingNibbles, nibbles, depth)

        if (common == leaf.remainingNibbles.size) {
            // Same key, update value
            return Node.Leaf(leaf.remainingNibbles, value)
        }

        // Need to branch
        val branch = Node.Branch(Array(16) { null })

        val leafIndex = leaf.remainingNibbles[common].toInt()
        val newLeafNibbles = leaf.remainingNibbles.copyOfRange(common + 1, leaf.remainingNibbles.size)
        branch.children[leafIndex] = Node.Leaf(newLeafNibbles, leaf.value)

        val keyIndex = nibbles[depth + common].toInt()
        val newKeyNibbles = nibbles.copyOfRange(depth + common + 1, nibbles.size)
        branch.children[keyIndex] = Node.Leaf(newKeyNibbles, value)

        return if (common > 0) {
            Node.Extension(nibbles.copyOfRange(depth, depth + common), branch)
        } else {
            branch
        }
    }

    private fun insertIntoExtension(extension: Node.Extension, nibbles: ByteArray, depth: Int, value: ByteArray): Node {
        val common = commonPrefix(extension.sharedNibbles, nibbles, depth)

        if (common == extension.sharedNibbles.size) {
            // Continue down the extension
            val newNext = insert(extension.next, nibbles, depth + common, value)
            return Node.Extension(extension.sharedNibbles, newNext)
        }

        // Need to split the extension
        val branch = Node.Branch(Array(16) { null })

        val extIndex = extension.sharedNibbles[common].toInt()
        val remainingExtNibbles = extension.sharedNibbles.copyOfRange(common + 1, extension.sharedNibbles.size)

        if (remainingExtNibbles.isEmpty()) {
            branch.children[extIndex] = extension.next
        } else {
            branch.children[extIndex] = Node.Extension(remainingExtNibbles, extension.next)
        }

        val keyIndex = nibbles[depth + common].toInt()
        val remainingKeyNibbles = nibbles.copyOfRange(depth + common + 1, nibbles.size)
        branch.children[keyIndex] = Node.Leaf(remainingKeyNibbles, value)

        return if (common > 0) {
            Node.Extension(nibbles.copyOfRange(depth, depth + common), branch)
        } else {
            branch
        }
    }

    private fun insertIntoBranch(branch: Node.Branch, nibbles: ByteArray, depth: Int, value: ByteArray): Node {
        if (depth == nibbles.size) {
            // Update value at branch
            return Node.Branch(branch.children, value)
        }

        val index = nibbles[depth].toInt()
        val child = branch.children[index]
        val newChild = insert(child, nibbles, depth + 1, value)

        val newChildren = branch.children.copyOf()
        newChildren[index] = newChild

        return Node.Branch(newChildren, branch.value)
    }

    private fun retrieve(node: Node?, nibbles: ByteArray, depth: Int): ByteArray? {
        if (node == null) return null

        return when (node) {
            is Node.Leaf -> {
                if (nibbles.contentEquals(node.remainingNibbles, depth)) {
                    node.value
                } else {
                    null
                }
            }

            is Node.Extension -> {
                if (!nibbles.startsWith(node.sharedNibbles, depth)) return null
                retrieve(node.next, nibbles, depth + node.sharedNibbles.size)
            }

            is Node.Branch -> {
                if (depth == nibbles.size) return node.value
                val index = nibbles[depth].toInt()
                retrieve(node.children[index], nibbles, depth + 1)
            }
        }
    }

    private fun remove(node: Node?, nibbles: ByteArray, depth: Int): Node? {
        if (node == null) return null

        return when (node) {
            is Node.Leaf -> {
                if (nibbles.contentEquals(node.remainingNibbles, depth)) {
                    null // Remove leaf
                } else {
                    node // Key doesn't exist
                }
            }

            is Node.Extension -> {
                if (!nibbles.startsWith(node.sharedNibbles, depth)) return node
                val newNext = remove(node.next, nibbles, depth + node.sharedNibbles.size)
                return if (newNext == null) {
                    null
                } else {
                    Node.Extension(node.sharedNibbles, newNext)
                }
            }

            is Node.Branch -> {
                if (depth == nibbles.size) {
                    // Remove value from branch
                    return Node.Branch(node.children, null)
                }

                val index = nibbles[depth].toInt()
                val child = node.children[index]
                val newChild = remove(child, nibbles, depth + 1)

                val newChildren = node.children.copyOf()
                newChildren[index] = newChild

                // Count non-null children
                val nonNullChildren = newChildren.count { it != null }
                val hasValue = node.value != null

                return when {
                    nonNullChildren == 0 && !hasValue -> null
                    nonNullChildren == 1 && !hasValue -> {
                        // Convert branch to extension/leaf
                        val onlyChildIndex = newChildren.indexOfFirst { it != null }
                        val onlyChild = newChildren[onlyChildIndex]!!

                        when (onlyChild) {
                            is Node.Leaf -> {
                                val combinedNibbles = byteArrayOf(onlyChildIndex.toByte()) + onlyChild.remainingNibbles
                                Node.Leaf(combinedNibbles, onlyChild.value)
                            }
                            is Node.Extension -> {
                                val combinedNibbles = byteArrayOf(onlyChildIndex.toByte()) + onlyChild.sharedNibbles
                                Node.Extension(combinedNibbles, onlyChild.next)
                            }
                            else -> Node.Branch(newChildren, node.value)
                        }
                    }
                    else -> Node.Branch(newChildren, node.value)
                }
            }
        }
    }

    // Helper functions
    private fun bytesToNibbles(bytes: ByteArray): ByteArray {
        val nibbles = ByteArray(bytes.size * 2)
        for (i in bytes.indices) {
            val byte = bytes[i].toInt() and 0xFF
            nibbles[i * 2] = (byte shr 4).toByte()
            nibbles[i * 2 + 1] = (byte and 0x0F).toByte()
        }
        return nibbles
    }

    private fun nibblesToBytes(nibbles: ByteArray): ByteArray {
        require(nibbles.size % 2 == 0) { "Nibbles must be even length" }
        val bytes = ByteArray(nibbles.size / 2)
        for (i in bytes.indices) {
            val high = nibbles[i * 2].toInt() and 0x0F
            val low = nibbles[i * 2 + 1].toInt() and 0x0F
            bytes[i] = ((high shl 4) or low).toByte()
        }
        return bytes
    }

    private fun commonPrefix(a: ByteArray, b: ByteArray, start: Int): Int {
        var i = 0
        while (i + start < a.size && i < b.size - start) {
            if (a[i + start] != b[i + start]) break
            i++
        }
        return i
    }

    private fun ByteArray.contentEquals(other: ByteArray, offset: Int): Boolean {
        if (this.size - offset != other.size) return false
        for (i in offset until this.size) {
            if (this[i] != other[i - offset]) return false
        }
        return true
    }

    private fun ByteArray.startsWith(prefix: ByteArray, offset: Int): Boolean {
        if (this.size - offset < prefix.size) return false
        for (i in prefix.indices) {
            if (this[offset + i] != prefix[i]) return false
        }
        return true
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            result.append(hexChars[value shr 4])
            result.append(hexChars[value and 0x0F])
        }
        return "0x" + result.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.removePrefix("0x")
        require(cleanHex.length % 2 == 0) { "Hex string must have even length" }

        val bytes = ByteArray(cleanHex.length / 2)
        for (i in bytes.indices) {
            val high = Character.digit(cleanHex[i * 2], 16)
            val low = Character.digit(cleanHex[i * 2 + 1], 16)
            bytes[i] = ((high shl 4) or low).toByte()
        }
        return bytes
    }
}