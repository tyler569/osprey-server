package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTInteger extends NBTValue {
    public static int ID = 3;
    int value;

    public NBTInteger(int v) {
        value = v;
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        Conversion.outputInteger(os, value);
    }
}
