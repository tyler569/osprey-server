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

    long readLong() throws IOException {
        return Protocol.readLong(this);
    }

    short readShort() throws IOException {
        return Protocol.readShort(this);
    }

    String readString() throws IOException {
        return Protocol.readString(this);
    }

    boolean readBoolean() {
        return read() != 0;
    }

    float readFloat() throws IOException {
        return Protocol.readFloat(this);
    }

    double readDouble() throws IOException {
        return Protocol.readDouble(this);
    }

    Location readLocation() throws IOException {
        return new Location(Protocol.readLong(this));
    }

    Position readPosition() throws IOException {
        var x = readDouble();
        var y = readDouble();
        var z = readDouble();
        var yaw = readFloat();
        var pitch = readFloat();
        return new Position(x, y, z, yaw, pitch);
    }
}
