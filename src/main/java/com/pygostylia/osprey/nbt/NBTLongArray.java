package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class NBTLongArray extends NBTValue {
    public static int ID = 12;
    List<Long> value;

    public NBTLongArray(Long... vs) {
        value = Arrays.asList(vs);
    }

    public NBTLongArray(Collection<Long> vs) {
        value = vs.stream().toList();
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        Conversion.outputInteger(os, value.size());
        for (var v : value) {
            Conversion.outputLong(os, v);
        }
    }

    @Override
    public String toString() {
        return "LongArray" + value.toString();
    }
}
