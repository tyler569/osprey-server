package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTDouble extends NBTValue {
    public static int ID = 6;
    double value;

    NBTDouble(String n, double v) {
        super(n);
        value = v;
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        Conversion.outputDouble(os, value);
    }
}
