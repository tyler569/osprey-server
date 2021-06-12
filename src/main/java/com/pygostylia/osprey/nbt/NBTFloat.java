package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;

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

    @Override
    public String toString() {
        return value + "f";
    }
}
