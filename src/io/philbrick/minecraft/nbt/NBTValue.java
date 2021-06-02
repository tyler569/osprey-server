package io.philbrick.minecraft.nbt;

import io.philbrick.minecraft.*;

import java.io.*;

public abstract class NBTValue {
    abstract public int id();
    abstract void encode(OutputStream os) throws IOException;
}
