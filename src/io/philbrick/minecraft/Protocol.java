package io.philbrick.minecraft;

import java.io.*;
import java.nio.*;

public class Protocol {
    private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN);

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

    public static int readInteger(InputStream is) throws IOException {
        var buffer = ByteBuffer.wrap(is.readNBytes(Integer.BYTES));
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.rewind();
        return buffer.getInt();
    }

    public static long readLong(InputStream is) throws IOException {
        var buffer = ByteBuffer.wrap(is.readNBytes(Long.BYTES));
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.rewind();
        return buffer.getLong();
    }

    public static float readFloat(InputStream is) throws IOException {
        var buffer = ByteBuffer.wrap(is.readNBytes(Float.BYTES));
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.rewind();
        return buffer.getFloat();
    }

    public static double readDouble(InputStream is) throws IOException {
        var buffer = ByteBuffer.wrap(is.readNBytes(Double.BYTES));
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.rewind();
        return buffer.getDouble();
    }

    // write
    // todo: these buffers should be at most thread-local

    public static void writeString(OutputStream os, String str) throws IOException {
        VarInt.write(os, str.length());
        os.write(str.getBytes());
    }

    public static void writeVarInt(OutputStream os, int number) throws IOException {
        VarInt.write(os, number);
    }

    public static void writeByte(OutputStream os, byte b) throws IOException {
        os.write(b);
    }

    public static void writeByte(OutputStream os, int b) throws IOException {
        os.write(b);
    }

    public static void writeBytes(OutputStream os, byte[] bytes) throws IOException {
        os.write(bytes);
    }

    public static void writeBoolean(OutputStream os, boolean b) throws IOException {
        os.write(b ? 1 : 0);
    }

    public static void writeShort(OutputStream os, short v) throws IOException {
        buffer.putShort(v);
        os.write(buffer.array(), 0, Short.BYTES);
        buffer.rewind();
    }

    public static void writeShort(OutputStream os, int v) throws IOException {
        buffer.putShort((short)v);
        os.write(buffer.array(), 0, Short.BYTES);
        buffer.rewind();
    }

    public static void writeInt(OutputStream os, int v) throws IOException {
        buffer.putInt(v);
        os.write(buffer.array(), 0, Integer.BYTES);
        buffer.rewind();
    }

    public static void writeLong(OutputStream os, long v) throws IOException {
        buffer.putLong(v);
        os.write(buffer.array(), 0, Long.BYTES);
        buffer.rewind();
    }

    public static void writeFloat(OutputStream os, float v) throws IOException {
        buffer.putFloat(v);
        os.write(buffer.array(), 0, Float.BYTES);
        buffer.rewind();
    }

    public static void writeDouble(OutputStream os, double v) throws IOException {
        buffer.putDouble(v);
        os.write(buffer.array(), 0, Double.BYTES);
        buffer.rewind();
    }
}
