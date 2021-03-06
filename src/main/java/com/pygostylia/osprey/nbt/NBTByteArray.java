package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class NBTByteArray extends NBTValue {
    public static int ID = 7;
    byte[] value;

    public NBTByteArray(byte[] v) {
        value = v;
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        Conversion.outputInteger(os, value.length);
        os.write(value);
    }

    @Override
    public String toString() {
        return Arrays.toString(value);
    }
}
