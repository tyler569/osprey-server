package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;

public class NBTString extends NBTValue {
    public static int ID = 8;
    String value;

    public NBTString(String v) {
        value = v;
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        Conversion.putModifiedString(os, value);
    }
}
