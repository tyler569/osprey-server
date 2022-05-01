package com.pygostylia.osprey;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class Packet extends ByteArrayInputStream {
    public int type;
    public int originalLen;
    public int compressedLen;

    public Packet(byte[] buf, int len) throws IOException {
        super(buf);
        originalLen = len;
    }

    public int readVarInt() throws IOException {
        return VarInt.read(this);
    }

    int readInteger() throws IOException {
        return Protocol.readInteger(this);
    }

    public long readLong() throws IOException {
        return Protocol.readLong(this);
    }

    public short readShort() throws IOException {
        return Protocol.readShort(this);
    }

    public String readString() throws IOException {
        return Protocol.readString(this);
    }

    public boolean readBoolean() {
        return read() != 0;
    }

    public float readFloat() throws IOException {
        return Protocol.readFloat(this);
    }

    public double readDouble() throws IOException {
        return Protocol.readDouble(this);
    }

    public Location readLocation() throws IOException {
        return new Location(Protocol.readLong(this));
    }

    public Position readPosition() throws IOException {
        var x = readDouble();
        var y = readDouble();
        var z = readDouble();
        var yaw = readFloat();
        var pitch = readFloat();
        return new Position(x, y, z, yaw, pitch);
    }
}
