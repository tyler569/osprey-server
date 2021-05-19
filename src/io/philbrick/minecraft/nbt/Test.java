package io.philbrick.minecraft.nbt;

import java.io.*;
import java.util.*;

public class Test {
    public static void main(String[] args) {
        var expected = new byte[] {
            0x0a, 0x00, 0x0b, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x20,
            0x77, 0x6f, 0x72, 0x6c, 0x64, 0x08, 0x00, 0x04, 0x6e,
            0x61, 0x6d, 0x65, 0x00, 0x09, 0x42, 0x61, 0x6e, 0x61,
            0x6e, 0x72, 0x61, 0x6d, 0x61, 0x00,
        };
        var small = new NBTCompound("hello world", new ArrayList<NBTValue>() {{
            add(new NBTString("name", "Bananrama"));
        }});
        var stream = new ByteArrayOutputStream();
        try {
            small.encode(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        var encoded = stream.toByteArray();
        System.out.println(Arrays.toString(encoded));
        System.out.println(Arrays.toString(expected));
        System.out.println(Arrays.equals(encoded, expected));
    }
}
