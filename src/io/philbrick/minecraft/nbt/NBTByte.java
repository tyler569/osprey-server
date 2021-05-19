package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTByte extends NBTValue {
    public static int ID = 1;
    byte value;

    NBTByte(String n, byte v) {
        super(n);
        value = v;
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        os.write(value);
    }
}
