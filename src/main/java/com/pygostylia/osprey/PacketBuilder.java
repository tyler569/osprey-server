package com.pygostylia.osprey;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class PacketBuilder extends ByteArrayOutputStream {
    public void writeString(String str) {
        VarInt.write(this, str.length());
        writeBytes(str.getBytes());
    }

    public void writeString0(String str) {
        writeBytes(str.getBytes());
        write(0);
    }

    public void writeVarInt(int number) {
        VarInt.write(this, number);
    }

    public void writeByte(byte b) {
        write(b);
    }

    public void writeBoolean(boolean b) {
        write(b ? 1 : 0);
    }

    public void writeShort(short v) {
        var buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putShort(v);
        write(buffer.array(), 0, Short.BYTES);
    }

    public void writeShort(int v) {
        var buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putShort((short) v);
        write(buffer.array(), 0, Short.BYTES);
    }

    public void writeInt(int v) {
        var buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putInt(v);
        write(buffer.array(), 0, Integer.BYTES);
    }

    public void writeLong(long v) {
        var buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(v);
        write(buffer.array(), 0, Long.BYTES);
    }

    public void writeFloat(float v) {
        var buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putFloat(v);
        write(buffer.array(), 0, Float.BYTES);
    }

    public void writeDouble(double v) {
        var buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putDouble(v);
        write(buffer.array(), 0, Double.BYTES);
    }

    public void writeUUID(UUID uuid) {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    public void writeLocation(int x, int y, int z) {
        long encoded = (((long) x & 0x3FFFFFF) << 38) | (((long) z & 0x3FFFFFF) << 12) | (y & 0xFFF);
        writeLong(encoded);
    }

    public void writeLocation(Location location) {
        writeLocation(location.getX(), location.getY(), location.getZ());
    }

    public void writePosition(Position position) {
        writeDouble(position.getX());
        writeDouble(position.getY());
        writeDouble(position.getZ());
        writeByte(position.pitchAngle());
        writeByte(position.yawAngle());
    }
}
