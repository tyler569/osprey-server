package io.philbrick.minecraft;

import java.io.*;
import java.util.ArrayList;

public class VarInt {
    public static int read(InputStream is) throws IOException {
        int result = 0;
        int numRead = 0;
        byte b;
        do {
            int v = is.read();
            if (v == -1) {
                throw new EOFException("EOF");
            }
            b = (byte)v;
            int value = b & 0b0111_1111;
            result |= value << (7 * numRead);
            numRead++;
        } while ((b & 0b1000_0000) != 0);
        return result;
    }

    public static void write(OutputStream os, int i) throws IOException {
        do {
            byte elem = (byte)(i & 0b0111_1111);
            i >>>= 7;
            if (i != 0) {
                elem |= 0b1000_0000;
            }
            os.write(elem);
        } while (i != 0);
    }

    public static int len(int i) {
        int len = 0;
        do {
            i >>>= 7;
            len += 1;
        } while (i != 0);
        return len;
    }

    static byte[] encode(int i) {
        var m = new ByteArrayOutputStream();
        try {
            write(m, i);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return m.toByteArray();
    }

}
