package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTShort extends NBTValue {
    public static int ID = 2;
    short value;

    NBTShort(short v) {
        value = v;
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        Conversion.outputShort(os, value);
    }
}
