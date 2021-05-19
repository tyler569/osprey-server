package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTLong extends NBTValue {
    public static int ID = 4;
    long value;

    NBTLong(String n, long v) {
        super(n);
        value = v;
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        Conversion.outputLong(os, value);
    }
}
