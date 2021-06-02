package io.philbrick.minecraft.nbt;

import java.io.*;

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
}
