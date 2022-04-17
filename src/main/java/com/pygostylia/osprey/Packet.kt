package com.pygostylia.osprey

import java.io.ByteArrayInputStream

class Packet private constructor(buf: ByteArray) : ByteArrayInputStream(buf) {
    @JvmField
    var originalLen: Int = 0

    @JvmField
    var type: Int = 0

    constructor(buf: ByteArray, len: Int) : this(buf) {
        originalLen = len
        type = readVarInt()
    }

    fun readVarInt(): Int {
        return VarInt.read(this)
    }

    fun readInteger(): Int {
        return Protocol.readInteger(this)
    }

    fun readLong(): Long {
        return Protocol.readLong(this)
    }

    fun readShort(): Short {
        return Protocol.readShort(this)
    }

    fun readString(): String {
        return Protocol.readString(this)
    }

    fun readBoolean(): Boolean {
        return read() != 0
    }

    fun readFloat(): Float {
        return Protocol.readFloat(this)
    }

    fun readDouble(): Double {
        return Protocol.readDouble(this)
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