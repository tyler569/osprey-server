package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

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

    @Override
    public String toString() {
        return "Array" + value.toString();
    }

    @Override
    public String toStringPretty(int depth) {
        var sb = new StringBuilder();
        sb.append("Array[\n");
        for (var v : value) {
            sb.append("    ".repeat(depth + 1));
            sb.append(v.toStringPretty(depth + 1));
            sb.append(",\n");
        }
        sb.append("    ".repeat(depth)).append("]");
        return sb.toString();
    }
}
