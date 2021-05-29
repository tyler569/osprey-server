package io.philbrick.minecraft;

import java.io.*;
import java.util.*;

public class PacketBuilder extends ByteArrayOutputStream {
    void writeString(String s) throws IOException {
        Protocol.writeString(this, s);
    }

    void writeByte(byte b) throws IOException {
        Protocol.writeByte(this, b);
    }

    void writeShort(short s) throws IOException {
        Protocol.writeShort(this, s);
    }

    void writeInt(int i) throws IOException {
        Protocol.writeInt(this, i);
    }

    void writeLong(long l) throws IOException {
        Protocol.writeLong(this, l);
    }

    void writeVarInt(int i) throws IOException {
        Protocol.writeVarInt(this, i);
    }

    void writeBoolean(boolean b) throws IOException {
        Protocol.writeBoolean(this, b);
    }

    void writeFloat(float f) throws IOException {
        Protocol.writeFloat(this, f);
    }

    void writeDouble(double d) throws IOException {
        Protocol.writeDouble(this, d);
    }

    void writePosition(int x, int y, int z) throws IOException {
        Protocol.writePosition(this, x, y, z);
    }

    void writePosition(Location l) throws IOException {
        Protocol.writeLong(this, l.encode());
    }

    void writeUUID(UUID uuid) throws IOException {
        Protocol.writeLong(this, uuid.getMostSignificantBits());
        Protocol.writeLong(this, uuid.getLeastSignificantBits());
    }

    void writePosition(Position position) throws IOException {
        Protocol.writeDouble(this, position.x);
        Protocol.writeDouble(this, position.y);
        Protocol.writeDouble(this, position.z);
        Protocol.writeByte(this, position.pitchAngle());
        Protocol.writeByte(this, position.yawAngle());
    }
}
