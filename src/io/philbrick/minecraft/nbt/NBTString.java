package io.philbrick.minecraft.nbt;

import java.io.*;
import java.nio.charset.*;

public class NBTString extends NBTValue {
    public static int ID = 8;
    String value;

    public NBTString(String n, String v) {
        super(n);
        value = v;
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        // Conversion.outputShort(os, (short)value.length());
        Conversion.putModifiedString(os, value);
    }
}
