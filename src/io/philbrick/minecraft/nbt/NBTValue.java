package io.philbrick.minecraft.nbt;

import io.philbrick.minecraft.*;

import java.io.*;

public abstract class NBTValue {
    String name;

    public NBTValue(String n) {
        name = n;
    }

    abstract public int id();
    abstract void innerEncode(OutputStream os) throws IOException;

    public void encode(OutputStream os) throws IOException {
        os.write(id());
        if (name != null) {
            Conversion.putModifiedString(os, name);
        } else {
            Conversion.outputShort(os, 0);
        }
        innerEncode(os);
    }
}
