package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;

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

    @Override
    public String toString() {
        return value + "s";
    }
}
