package com.pygostylia.osprey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class PacketBuilder extends ByteArrayOutputStream {
    public void writeString(String s) throws IOException {
        Protocol.writeString(this, s);
    }

    public void writeByte(byte b) throws IOException {
        Protocol.writeByte(this, b);
    }

    public void writeShort(short s) throws IOException {
        Protocol.writeShort(this, s);
    }

    public void writeInt(int i) throws IOException {
        Protocol.writeInt(this, i);
    }

    public void writeLong(long l) throws IOException {
        Protocol.writeLong(this, l);
    }

    public void writeVarInt(int i) throws IOException {
        Protocol.writeVarInt(this, i);
    }

    public void writeBoolean(boolean b) throws IOException {
        Protocol.writeBoolean(this, b);
    }

    public void writeFloat(float f) throws IOException {
        Protocol.writeFloat(this, f);
    }

    public void writeDouble(double d) throws IOException {
        Protocol.writeDouble(this, d);
    }

    public void writePosition(int x, int y, int z) throws IOException {
        Protocol.writePosition(this, x, y, z);
    }

    public void writePosition(Location l) throws IOException {
        Protocol.writeLong(this, l.encode());
    }

    public void writeUUID(UUID uuid) throws IOException {
        Protocol.writeLong(this, uuid.getMostSignificantBits());
        Protocol.writeLong(this, uuid.getLeastSignificantBits());
    }

    public void writePosition(Position position) throws IOException {
        Protocol.writeDouble(this, position.x);
        Protocol.writeDouble(this, position.y);
        Protocol.writeDouble(this, position.z);
        Protocol.writeByte(this, position.pitchAngle());
        Protocol.writeByte(this, position.yawAngle());
    }
}
