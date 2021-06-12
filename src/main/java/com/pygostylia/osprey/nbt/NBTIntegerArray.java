package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

    @Override
    public String toString() {
        return "IntegerArray" + value.toString();
    }
}
