package io.philbrick.minecraft.nbt;

import java.io.*;
import java.util.*;

public class NBTLongArray extends NBTValue {
    public static int ID = 12;
    List<Long> value;

    public NBTLongArray(String n, Long... vs) {
        super(n);
        value = Arrays.asList(vs);
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        Conversion.outputInteger(os, value.size());
        for (var v : value) {
            Conversion.outputLong(os, v);
        }
    }
}
