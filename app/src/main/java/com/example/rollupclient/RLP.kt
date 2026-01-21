package com.example.rollupclient

import java.math.BigInteger

/**
 * Recursive Length Prefix (RLP) encoding as per Ethereum
 * Simplified implementation
 */
object RLP {

    fun encode(value: ByteArray): ByteArray {
        return when {
            value.size == 1 && value[0] < 0x80.toByte() -> value
            value.size < 56 -> encodeShortString(value)
            else -> encodeLongString(value)
        }
    }

    fun encodeString(str: String): ByteArray {
        return encode(str.toByteArray())
    }

    fun encodeBigInteger(value: BigInteger): ByteArray {
        return if (value == BigInteger.ZERO) {
            byteArrayOf()
        } else {
            encode(value.toByteArray().dropWhile { it == 0.toByte() }.toByteArray())
        }
    }

    fun encodeList(vararg elements: ByteArray): ByteArray {
        val totalLength = elements.sumOf { it.size }
        val result = ByteArray(totalLength + if (totalLength < 56) 1 else 2 + getLengthBytes(totalLength).size)

        var offset = 0
        if (totalLength < 56) {
            result[offset++] = (0xc0 + totalLength).toByte()
        } else {
            val lengthBytes = getLengthBytes(totalLength)
            result[offset++] = (0xc0 + 55 + lengthBytes.size).toByte()
            System.arraycopy(lengthBytes, 0, result, offset, lengthBytes.size)
            offset += lengthBytes.size
        }

        for (element in elements) {
            System.arraycopy(element, 0, result, offset, element.size)
            offset += element.size
        }

        return result
    }

    private fun encodeShortString(value: ByteArray): ByteArray {
        val result = ByteArray(value.size + 1)
        result[0] = (0x80 + value.size).toByte()
        System.arraycopy(value, 0, result, 1, value.size)
        return result
    }

    private fun encodeLongString(value: ByteArray): ByteArray {
        val lengthBytes = getLengthBytes(value.size)
        val result = ByteArray(value.size + 1 + lengthBytes.size)
        result[0] = (0x80 + 55 + lengthBytes.size).toByte()
        System.arraycopy(lengthBytes, 0, result, 1, lengthBytes.size)
        System.arraycopy(value, 0, result, 1 + lengthBytes.size, value.size)
        return result
    }

    private fun getLengthBytes(length: Int): ByteArray {
        return if (length == 0) {
            byteArrayOf()
        } else {
            val bytes = ByteArray(4)
            for (i in 3 downTo 0) {
                bytes[3 - i] = (length shr (8 * i)).toByte()
            }
            bytes.dropWhile { it == 0.toByte() }.toByteArray()
        }
    }

    // Decoding
    fun decodeToList(data: ByteArray): List<ByteArray> {
        if (data.isEmpty()) return emptyList()

        val firstByte = data[0].toInt() and 0xFF
        return when {
            firstByte <= 0xbf -> {
                val item = decodeItem(data, 0)
                listOf(item) + decodeToList(data.copyOfRange(item.size, data.size))
            }
            firstByte <= 0xf7 -> {
                val listLength = firstByte - 0xc0
                val listData = data.copyOfRange(1, 1 + listLength)
                decodeListItems(listData)
            }
            else -> {
                val lengthOfLength = firstByte - 0xf7
                val lengthBytes = data.copyOfRange(1, 1 + lengthOfLength)
                val listLength = bytesToInt(lengthBytes)
                val listData = data.copyOfRange(1 + lengthOfLength, 1 + lengthOfLength + listLength)
                decodeListItems(listData)
            }
        }
    }

    private fun decodeItem(data: ByteArray, start: Int): ByteArray {
        val firstByte = data[start].toInt() and 0xFF
        return when {
            firstByte < 0x80 -> byteArrayOf(data[start])
            firstByte <= 0xb7 -> {
                val length = firstByte - 0x80
                data.copyOfRange(start + 1, start + 1 + length)
            }
            else -> {
                val lengthOfLength = firstByte - 0xb7
                val lengthBytes = data.copyOfRange(start + 1, start + 1 + lengthOfLength)
                val length = bytesToInt(lengthBytes)
                data.copyOfRange(start + 1 + lengthOfLength, start + 1 + lengthOfLength + length)
            }
        }
    }

    private fun decodeListItems(data: ByteArray): List<ByteArray> {
        val items = mutableListOf<ByteArray>()
        var offset = 0

        while (offset < data.size) {
            val item = decodeItem(data, offset)
            items.add(item)
            offset += when {
                item.size == 1 && item[0] < 0x80.toByte() -> 1
                item.size < 56 -> item.size + 1
                else -> {
                    val lengthOfLength = data[offset].toInt() and 0xFF - 0xb7
                    item.size + 1 + lengthOfLength
                }
            }
        }

        return items
    }

    private fun bytesToInt(bytes: ByteArray): Int {
        var result = 0
        for (byte in bytes) {
            result = (result shl 8) or (byte.toInt() and 0xFF)
        }
        return result
    }
}