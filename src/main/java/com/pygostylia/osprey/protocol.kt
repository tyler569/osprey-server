package com.pygostylia.osprey

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun readString(i: InputStream): String {
    val stringLen = VarInt.read(i)
    val data = i.readNBytes(stringLen)
    return String(data)
}

private fun bytes(i: InputStream, n: Int): ByteBuffer {
    val buffer = ByteBuffer.wrap(i.readNBytes(n))
    buffer.order(ByteOrder.BIG_ENDIAN)
    return buffer
}

fun readShort(i: InputStream): Short {
    val buffer = bytes(i, Short.SIZE_BYTES)
    return buffer.short
}

fun readInt(i: InputStream): Int {
    val buffer = bytes(i, Int.SIZE_BYTES)
    return buffer.int
}

fun readLong(i: InputStream): Long {
    val buffer = bytes(i, Long.SIZE_BYTES)
    return buffer.long
}

fun readFloat(i: InputStream): Float {
    val buffer = bytes(i, Float.SIZE_BYTES)
    return buffer.float
}

fun readDouble(i: InputStream): Double {
    val buffer = bytes(i, Double.SIZE_BYTES)
    return buffer.double
}