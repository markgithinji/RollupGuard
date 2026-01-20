package com.example.rollupclient

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Merkle Tree implementation from scratch - used for state proofs
 */
class MerkleTree {

    data class Proof(
        val leaf: ByteArray,
        val leafIndex: Int,
        val siblings: List<ByteArray>,
        val root: ByteArray
    )

    companion object {
        /**
         * Keccak256 implementation (simplified - in production use proper library)
         */
        fun keccak256(input: ByteArray): ByteArray {
            // Note: For production, use proper Keccak implementation
            // This is a simplified version for learning
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(input)
        }

        fun keccak256(vararg inputs: ByteArray): ByteArray {
            val combined = inputs.flatMap { it.toList() }.toByteArray()
            return keccak256(combined)
        }

        /**
         * Build a Merkle tree from leaves
         */
        fun buildTree(leaves: List<ByteArray>): List<List<ByteArray>> {
            if (leaves.isEmpty()) return emptyList()

            val tree = mutableListOf<List<ByteArray>>()
            tree.add(leaves)

            var currentLevel = leaves
            while (currentLevel.size > 1) {
                val nextLevel = mutableListOf<ByteArray>()

                for (i in currentLevel.indices step 2) {
                    val left = currentLevel[i]
                    val right = if (i + 1 < currentLevel.size) {
                        currentLevel[i + 1]
                    } else {
                        // Duplicate last element if odd number
                        currentLevel[i]
                    }

                    val parent = keccak256(left, right)
                    nextLevel.add(parent)
                }

                tree.add(nextLevel)
                currentLevel = nextLevel
            }

            return tree
        }

        /**
         * Get Merkle root
         */
        fun getRoot(leaves: List<ByteArray>): ByteArray {
            val tree = buildTree(leaves)
            return tree.last().first()
        }

        /**
         * Generate inclusion proof for a leaf
         */
        fun generateProof(leaves: List<ByteArray>, leafIndex: Int): Proof {
            val tree = buildTree(leaves)

            if (leafIndex >= leaves.size) {
                throw IllegalArgumentException("Leaf index out of bounds")
            }

            val siblings = mutableListOf<ByteArray>()
            var currentIndex = leafIndex

            // Walk up the tree collecting siblings
            for (level in 0 until tree.size - 1) {
                val levelNodes = tree[level]

                val isRightNode = currentIndex % 2 == 1
                val siblingIndex = if (isRightNode) {
                    currentIndex - 1
                } else {
                    if (currentIndex + 1 < levelNodes.size) {
                        currentIndex + 1
                    } else {
                        // If no right sibling, duplicate left (odd level case)
                        currentIndex
                    }
                }

                siblings.add(levelNodes[siblingIndex])
                currentIndex /= 2
            }

            return Proof(
                leaf = leaves[leafIndex],
                leafIndex = leafIndex,
                siblings = siblings,
                root = tree.last().first()
            )
        }

        /**
         * Verify Merkle proof
         */
        fun verifyProof(proof: Proof): Boolean {
            var computedHash = proof.leaf

            var index = proof.leafIndex
            for (sibling in proof.siblings) {
                computedHash = if (index % 2 == 0) {
                    // Current is left node
                    keccak256(computedHash, sibling)
                } else {
                    // Current is right node
                    keccak256(sibling, computedHash)
                }
                index /= 2
            }

            return computedHash.contentEquals(proof.root)
        }

        /**
         * Convert hex string to byte array
         */
        fun hexStringToByteArray(hex: String): ByteArray {
            val cleanHex = hex.removePrefix("0x")
            val len = cleanHex.length
            val data = ByteArray(len / 2)

            for (i in 0 until len step 2) {
                data[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4) +
                        Character.digit(cleanHex[i + 1], 16)).toByte()
            }

            return data
        }

        /**
         * Convert byte array to hex string
         */
        fun bytesToHex(bytes: ByteArray): String {
            val hexChars = "0123456789abcdef"
            val result = StringBuilder(bytes.size * 2)

            for (byte in bytes) {
                val v = byte.toInt() and 0xFF
                result.append(hexChars[v shr 4])
                result.append(hexChars[v and 0x0F])
            }

            return "0x${result.toString()}"
        }
    }
}