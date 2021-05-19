package io.philbrick.minecraft.nbt;

import java.io.*;

public class NBTString extends NBTValue {
    public static int ID = 8;
    String value;

    NBTString(String n, String v) {
        super(n);
        value = v;
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        Conversion.outputShort(os, (short)value.length());
        os.write(value.getBytes());
    }
}
