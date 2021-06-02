package io.philbrick.minecraft.nbt;

import java.io.*;
import java.util.*;

public class NBTList<T extends NBTValue> extends NBTValue {
    public static int ID = 9;
    ArrayList<T> value;

    public NBTList() {
        value = new ArrayList<>();
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        if (value.isEmpty()) {
            os.write(0);
        } else {
            os.write(value.get(0).id());
        }
        Conversion.outputInteger(os, value.size());
        if (value.size() == 0) {
            (new NBTEnd()).encode(os);
            return;
        }
        for (T v : value) {
            v.encode(os);
        }
    }

    public boolean add(T v) {
        return value.add(v);
    }
}
