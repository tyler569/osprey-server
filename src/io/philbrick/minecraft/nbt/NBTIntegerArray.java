package io.philbrick.minecraft.nbt;

import java.io.*;
import java.util.*;

public class NBTIntegerArray extends NBTValue {
    public static int ID = 11;
    List<Integer> value;

    public NBTIntegerArray(String n, Integer... vs) {
        super(n);
        value = Arrays.asList(vs);
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        Conversion.outputInteger(os, value.size());
        for (var v : value) {
            Conversion.outputInteger(os, v);
        }
    }
}
