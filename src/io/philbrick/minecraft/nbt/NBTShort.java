package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTShort extends NBTValue {
    public static int ID = 2;
    short value;

    NBTShort(String n, short v) {
        super(n);
        value = v;
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        Conversion.outputShort(os, value);
    }
}
