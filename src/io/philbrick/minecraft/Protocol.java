package io.philbrick.minecraft;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;

public class Protocol {
    public static String readString(InputStream is) throws IOException {
        int stringLen = VarInt.read(is);
        var data = is.readNBytes(stringLen);
        return new String(data);
    }

    public static short readShort(InputStream is) throws IOException {
        var buffer = ByteBuffer.wrap(is.readNBytes(Short.BYTES));
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.rewind();
        return buffer.getShort();
    }

    public static long readLong(InputStream is) throws IOException {
        var buffer = ByteBuffer.wrap(is.readNBytes(Long.BYTES));
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.rewind();
        return buffer.getLong();
    }

    public static void writeString(OutputStream os, String str) throws IOException {
        VarInt.write(str.length(), os);
        os.write(str.getBytes());
    }

    public static void writeVarInt(OutputStream os, int number) throws IOException {
        VarInt.write(number, os);
    }

    public static void writeBytes(OutputStream os, byte[] bytes) throws IOException {
        os.write(bytes);
    }

    public static void writeLong(OutputStream os, long number) throws IOException {
        var buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(number);
        os.write(buffer.array());
    }

    public static void writeBoolean(OutputStream os, boolean b) throws IOException {
        os.write(b ? 1 : 0);
    }

    public static void writeInt(OutputStream os, int number) throws IOException {
        var buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(number);
        os.write(buffer.array());
    }

    public static void writeShort(OutputStream os, short number) throws IOException {
        var buffer = ByteBuffer.allocate(Short.BYTES);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(number);
        os.write(buffer.array());
    }

    public static void writeByte(OutputStream os, byte number) throws IOException {
        os.write(number);
    }
}
