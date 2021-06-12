package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;

public class NBTLong extends NBTValue {
    public static int ID = 4;
    long value;

    NBTLong(long v) {
        value = v;
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        Conversion.outputLong(os, value);
    }

    @Override
    public String toString() {
        return value + "L";
    }
}
