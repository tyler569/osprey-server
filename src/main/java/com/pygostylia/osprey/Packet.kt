package com.pygostylia.osprey

import java.io.ByteArrayInputStream
import java.io.DataInputStream

class Packet(buf: ByteArray, len: Int) : DataInputStream(ByteArrayInputStream(buf)) {
    @JvmField
    var originalLen: Int = 0

    @JvmField
    var type: Int = 0

    init {
        originalLen = len
        type = readVarInt()
    }

    fun readVarInt(): Int {
        return VarInt.read(this)
    }

    fun readString(): String {
        return Protocol.readString(this)
    }

    fun readLocation(): BlockPosition {
        return BlockPosition(Protocol.readLong(this))
    }

    fun readPosition(): EntityPosition {
        val x = readDouble()
        val y = readDouble()
        val z = readDouble()
        val yaw = readFloat().toDouble()
        val pitch = readFloat().toDouble()
        return EntityPosition(x, y, z, yaw, pitch)
    }
}