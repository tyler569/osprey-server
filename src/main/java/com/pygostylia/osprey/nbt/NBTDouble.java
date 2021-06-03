package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;

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
