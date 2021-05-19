package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTEnd extends NBTValue {
    public static int ID = 0;

    public NBTEnd() {
        super(null);
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        os.write(0);
    }

    public void encode(OutputStream os) throws IOException {
        os.write(0);
    }
}
