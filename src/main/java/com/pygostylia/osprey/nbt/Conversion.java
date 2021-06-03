package com.pygostylia.osprey.nbt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Conversion {
    static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN);

    public static void outputShort(OutputStream os, short v) throws IOException {
        buffer.putShort(v);
        os.write(buffer.array(), 0, Short.BYTES);
        buffer.rewind();
    }

    public static void outputShort(OutputStream os, int v) throws IOException {
        buffer.putShort((short)v);
        os.write(buffer.array(), 0, Short.BYTES);
        buffer.rewind();
    }

    public static void outputInteger(OutputStream os, int v) throws IOException {
        buffer.putInt(v);
        os.write(buffer.array(), 0, Integer.BYTES);
        buffer.rewind();
    }

    public static void outputLong(OutputStream os, long v) throws IOException {
        buffer.putLong(v);
        os.write(buffer.array(), 0, Long.BYTES);
        buffer.rewind();
    }

    public static void outputFloat(OutputStream os, float v) throws IOException {
        buffer.putFloat(v);
        os.write(buffer.array(), 0, Float.BYTES);
        buffer.rewind();
    }

    public static void outputDouble(OutputStream os, double v) throws IOException {
        buffer.putDouble(v);
        os.write(buffer.array(), 0, Double.BYTES);
        buffer.rewind();
    }

    public static void putModifiedString(OutputStream os, String str) throws IOException {
        DataOutputStream dso = new DataOutputStream(os);
        dso.writeUTF(str);
        dso.close();
    }
}