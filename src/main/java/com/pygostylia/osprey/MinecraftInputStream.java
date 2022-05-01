package com.pygostylia.osprey;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MinecraftInputStream extends DataInputStream {
    public int type;
    public int originalLen;
    public int compressedLen;

    public MinecraftInputStream(byte[] buf, int len) throws IOException {
        super(new ByteArrayInputStream(buf));
        originalLen = len;
    }

    public MinecraftInputStream(InputStream is) throws IOException {
        super(is);
    }

    public int readVarInt() throws IOException {
        return VarInt.read(this);
    }

    public String readString() throws IOException {
        int length = readVarInt();
        byte[] data = new byte[length];
        read(data);
        return new String(data);
    }

    public BlockPosition readLocation() throws IOException {
        return new BlockPosition(Protocol.readLong(this));
    }

    public EntityPosition readPosition() throws IOException {
        var x = readDouble();
        var y = readDouble();
        var z = readDouble();
        var yaw = readFloat();
        var pitch = readFloat();
        return new EntityPosition(x, y, z, yaw, pitch);
    }
}
