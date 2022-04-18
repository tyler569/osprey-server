package com.pygostylia.osprey.streams

import com.pygostylia.osprey.BlockPosition
import com.pygostylia.osprey.EntityPosition
import com.pygostylia.osprey.Protocol
import com.pygostylia.osprey.VarInt
import java.io.DataInputStream
import java.io.InputStream

class MinecraftInputStream(i: InputStream) : DataInputStream(i) {
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