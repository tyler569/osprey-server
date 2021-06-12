package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;

public abstract class NBTValue {
    abstract public int id();

    abstract void encode(OutputStream os) throws IOException;

    public String toStringPretty(int depth) {
        return toString();
    }
}
