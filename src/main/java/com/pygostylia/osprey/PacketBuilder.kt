package com.pygostylia.osprey

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*

class PacketBuilder : ByteArrayOutputStream() {
    fun writeString(str: String) {
        VarInt.write(this, str.length)
        writeBytes(str.toByteArray())
    }

    fun writeString0(str: String) {
        writeBytes(str.toByteArray())
        write(0)
    }

    fun writeVarInt(number: Int) {
        VarInt.write(this, number)
    }

    fun writeByte(b: Byte) {
        write(b.toInt())
    }

    fun writeBoolean(b: Boolean) {
        write(if (b) 1 else 0)
    }

    fun writeShort(v: Short) {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putShort(v)
        write(buffer.array(), 0, java.lang.Short.BYTES)
    }

    fun writeShort(v: Int) {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putShort(v.toShort())
        write(buffer.array(), 0, java.lang.Short.BYTES)
    }

    fun writeInt(v: Int) {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putInt(v)
        write(buffer.array(), 0, Integer.BYTES)
    }

    fun writeLong(v: Long) {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putLong(v)
        write(buffer.array(), 0, java.lang.Long.BYTES)
    }

    fun writeFloat(v: Float) {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putFloat(v)
        write(buffer.array(), 0, java.lang.Float.BYTES)
    }

    fun writeDouble(v: Double) {
        val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
        buffer.putDouble(v)
        write(buffer.array(), 0, java.lang.Double.BYTES)
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
}