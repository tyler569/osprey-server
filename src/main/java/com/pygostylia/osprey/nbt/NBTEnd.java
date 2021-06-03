package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;

public class NBTEnd extends NBTValue {
    public static int ID = 0;

    public NBTEnd() {
    }

    public int id() {
        return ID;
    }

    void encode(OutputStream os) throws IOException {
        os.write(0);
    }
}
