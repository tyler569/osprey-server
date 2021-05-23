package io.philbrick.minecraft;

import java.io.*;

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
}
