package io.philbrick.minecraft.nbt;

import java.io.*;

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
}
