package com.pygostylia.osprey;

import java.io.*;
import java.util.UUID;

public class MinecraftOutputStream extends DataOutputStream {
    public MinecraftOutputStream(OutputStream out) {
        super(out);
    }

    public MinecraftOutputStream() {
        super(new ByteArrayOutputStream());
    }

    public void writeString(String str) throws IOException {
        VarInt.write(this, str.length());
        writeBytes(str);
    }

    public void writeString0(String str) throws IOException {
        writeBytes(str);
        write(0);
    }

    public void writeVarInt(int number) throws IOException {
        VarInt.write(this, number);
    }

    public void writeByte(byte b) throws IOException {
        write(b);
    }

    public void writeUUID(UUID uuid) throws IOException {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    public void writeLocation(int x, int y, int z) throws IOException {
        long encoded = (((long) x & 0x3FFFFFF) << 38) | (((long) z & 0x3FFFFFF) << 12) | (y & 0xFFF);
        writeLong(encoded);
    }

    public void writeLocation(BlockPosition blockPosition) throws IOException {
        writeLocation(blockPosition.x(), blockPosition.y(), blockPosition.z());
    }

    public void writePosition(EntityPosition entityPosition) throws IOException {
        writeDouble(entityPosition.x);
        writeDouble(entityPosition.y);
        writeDouble(entityPosition.z);
        writeByte(entityPosition.pitchAngle());
        writeByte(entityPosition.yawAngle());
    }

    public byte[] toByteArray() {
        if (out instanceof ByteArrayOutputStream bout) {
            return bout.toByteArray();
        } else {
            return null;
        }
    }
}
