package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTByteArray extends NBTValue {
    public static int ID = 7;
    byte[] value;

    public NBTByteArray(byte[] v) {
        value = v;
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        Conversion.outputInteger(os, value.length);
        os.write(value);
    }
}
