package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;

public class NBTByte extends NBTValue {
    public static int ID = 1;
    byte value;

    public NBTByte(byte v) {
        value = v;
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        os.write(value);
    }

    @Override
    public String toString() {
        return value + "b";
    }
}
