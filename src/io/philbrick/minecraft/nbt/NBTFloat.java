package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTFloat extends NBTValue {
    public static int ID = 5;
    float value;

    public NBTFloat(float v) {
        value = v;
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        Conversion.outputFloat(os, value);
    }
}
