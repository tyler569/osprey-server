package io.philbrick.minecraft.nbt;

import java.io.*;
import java.util.*;

public class NBTIntegerArray extends NBTValue {
    public static int ID = 11;
    List<Integer> value;

    public NBTIntegerArray(Integer... vs) {
        value = Arrays.asList(vs);
    }

    public NBTIntegerArray(Collection<Integer> vs) {
        value = vs.stream().toList();
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        Conversion.outputInteger(os, value.size());
        for (var v : value) {
            Conversion.outputInteger(os, v);
        }
    }

    public boolean add(int e) {
        return value.add(e);
    }
}
