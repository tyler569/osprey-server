package com.pygostylia.osprey

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.util.*

class PacketBuilder : DataOutputStream(ByteArrayOutputStream()) {
    fun writeString(str: String) {
        VarInt.write(this, str.length)
        write(str.toByteArray())
    }

    fun writeVarInt(number: Int) {
        VarInt.write(this, number)
    }

    fun writeByte(b: Byte) {
        write(b.toInt())
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
        writeByte(entityPosition.pitchAngle())
        writeByte(entityPosition.yawAngle())
    }

    fun toByteArray(): ByteArray = (out as ByteArrayOutputStream).toByteArray()
}