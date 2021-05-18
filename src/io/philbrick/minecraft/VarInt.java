package io.philbrick.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class VarInt {
    static int read(InputStream is) throws IOException {
        int result = 0;
        int numRead = 0;
        byte b;
        do {
            b = (byte)is.read();
            int value = b & 0b0111_1111;
            result |= value << (7 * numRead);
            numRead++;
        } while ((b & 0b1000_0000) != 0);
        return result;
    }

    static void write(int i, OutputStream os) throws IOException {
        var arr = new ArrayList<Byte>();
        do {
            byte elem = (byte)(i & 0b0111_1111);
            i >>>= 7;
            if (i != 0) {
                elem |= 0b1000_0000;
            }
            os.write(elem);
        } while (i != 0);
    }

    static byte[] encode(int i) {
        var m = new ByteArrayOutputStream();
        try {
            write(i, m);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return m.toByteArray();
    }

}
