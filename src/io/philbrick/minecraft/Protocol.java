package io.philbrick.minecraft;

import java.io.*;
import java.nio.*;

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
}
