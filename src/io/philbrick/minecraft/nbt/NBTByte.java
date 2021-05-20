package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTByte extends NBTValue {
    public static int ID = 1;
    byte value;

    public NBTByte(String n, byte v) {
        super(n);
        value = v;
    }

    public NBTByte(String n, int v) {
        super(n);
        value = (byte)v;
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        os.write(value);
    }
}
