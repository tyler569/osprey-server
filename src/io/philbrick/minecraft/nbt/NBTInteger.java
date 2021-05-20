package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTInteger extends NBTValue {
    public static int ID = 3;
    int value;

    public NBTInteger(String n, int v) {
        super(n);
        value = v;
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        Conversion.outputInteger(os, value);
    }
}
