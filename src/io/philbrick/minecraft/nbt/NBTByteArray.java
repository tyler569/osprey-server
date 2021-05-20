package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTByteArray extends NBTValue {
    public static int ID = 7;
    byte[] value;

    public NBTByteArray(String n, byte[] v) {
        super(n);
        value = v;
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        Conversion.outputInteger(os, value.length);
        os.write(value);
    }
}
