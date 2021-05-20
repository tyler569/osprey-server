package io.philbrick.minecraft.nbt;

import java.io.*;
import java.util.*;

public class NBTList<T extends NBTValue> extends NBTValue {
    public static int ID = 9;
    List<T> value;

    NBTList(String n, ArrayList<T> v) {
        super(n);
        value = v;
    }

    NBTList(String n, T... vs) {
        super(n);
        value = Arrays.asList(vs);
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        if (value.isEmpty()) {
            os.write(0);
        } else {
            os.write(value.get(0).id());
        }
        Conversion.outputInteger(os, value.size());
        if (value.size() == 0) {
            (new NBTEnd()).innerEncode(os);
            return;
        }
        for (T v : value) {
            v.innerEncode(os);
        }
    }
}
