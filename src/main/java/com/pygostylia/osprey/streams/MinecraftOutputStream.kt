package com.pygostylia.osprey.streams

import com.pygostylia.osprey.BlockPosition
import com.pygostylia.osprey.EntityPosition
import com.pygostylia.osprey.VarInt
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.*

class MinecraftOutputStream(o: OutputStream) : DataOutputStream(o) {
    constructor() : this(ByteArrayOutputStream())

    fun writeString(str: String) {
        VarInt.write(this, str.length)
        write(str.toByteArray())
    }

    fun writeVarInt(number: Int) {
        VarInt.write(this, number)
    }

    fun writeUUID(uuid: UUID) {
        writeLong(uuid.mostSignificantBits)
        writeLong(uuid.leastSignificantBits)
    }

    fun writeBlockPosition(b: BlockPosition) {
        writeLong(b.encode())
    }

    fun writeBlockPosition(x: Int, y: Int, z: Int) {
        val pos = BlockPosition(x, y, z)
        writeBlockPosition(pos)
    }

    fun writePosition(entityPosition: EntityPosition) {
        writeDouble(entityPosition.x)
        writeDouble(entityPosition.y)
        writeDouble(entityPosition.z)
        writeByte(entityPosition.pitchAngle().toInt())
        writeByte(entityPosition.yawAngle().toInt())
    }

    fun toByteArray(): ByteArray = (out as ByteArrayOutputStream).toByteArray()
}