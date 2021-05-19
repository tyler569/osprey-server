package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTFloat extends NBTValue {
    public static int ID = 5;
    float value;

    NBTFloat(String n, float v) {
        super(n);
        value = v;
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        Conversion.outputFloat(os, value);
    }
}
