package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTDouble extends NBTValue {
    public static int ID = 6;
    double value;

    public NBTDouble(double v) {
        value = v;
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        Conversion.outputDouble(os, value);
    }
}
