package io.philbrick.minecraft.nbt;

import java.io.*;
import java.util.*;

public class NBTCompound extends NBTValue {
    public static int ID = 10;
    List<NBTValue> value;

    NBTCompound(String n, ArrayList<NBTValue> v) {
        super(n);
        value = v;
    }

    NBTCompound(String n, NBTValue... vs) {
        super(n);
        value = Arrays.asList(vs);
    }

    public int id() {
        return ID;
    }

    public void innerEncode(OutputStream os) throws IOException {
        for (NBTValue v : value) {
            v.encode(os);
        }
        (new NBTEnd()).encode(os);
        return;
    }
}
