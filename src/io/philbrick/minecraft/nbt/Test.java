package io.philbrick.minecraft.nbt;

import java.io.*;
import java.util.*;

public class Test {
    public static void main(String[] args) throws IOException {
        small();
        large();
    }

    public static void large() throws IOException {
        var large = new NBTCompound("Level",  new ArrayList<>() {{
            add(new NBTCompound("nested compound test",  new ArrayList<>() {{
                add(new NBTCompound("egg",  new ArrayList<>() {{
                    add(new NBTString("name",  "Eggbert"));
                    add(new NBTFloat("value",  0.5f));
                }}));
                add(new NBTCompound("ham",  new ArrayList<>() {{
                    add(new NBTString("name",  "Hampus"));
                    add(new NBTFloat("value",  0.75f));
                }}));
            }}));
            add(new NBTInteger("intTest",  2147483647));
            add(new NBTByte("byteTest",  (byte)127));
            add(new NBTString("stringTest",  "HELLO WORLD THIS IS A TEST STRING \303\205\303\204\303\226!"));
            add(new NBTList<>("listTest (long)",  new ArrayList<>() {{
                add(new NBTLong(null,  11));
                add(new NBTLong(null,  12));
                add(new NBTLong(null,  13));
                add(new NBTLong(null,  14));
                add(new NBTLong(null,  15));
            }}));
            add(new NBTDouble("doubleTest",  0.49312871321823148));
            add(new NBTFloat("floatTest",  0.49823147058486938f));
            add(new NBTLong("longTest",  9223372036854775807L));
            add(new NBTList<>("listTest (compound)",  new ArrayList<>() {{
                add(new NBTCompound(null,  new ArrayList<>() {{
                    add(new NBTLong("created-on",  1264099775885L));
                    add(new NBTString("name",  "Compound tag #0"));
                }}));
                add(new NBTCompound(null,  new ArrayList<>() {{
                    add(new NBTLong("created-on",  1264099775885L));
                    add(new NBTString("name",  "Compound tag #1"));
                }}));
            }}));
            add(new NBTByteArray("byteArrayTest", new byte[] { 1, 2, 3 }));
            add(new NBTShort("shortTest",  (short)32767));
        }});

        var stream = new ByteArrayOutputStream();
        try {
            large.encode(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        var encoded = stream.toByteArray();
        System.out.println(Arrays.toString(encoded));
    }

    public static void small() {
        var expected = new byte[]{
                0x0a, 0x00, 0x0b, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x20,
                0x77, 0x6f, 0x72, 0x6c, 0x64, 0x08, 0x00, 0x04, 0x6e,
                0x61, 0x6d, 0x65, 0x00, 0x09, 0x42, 0x61, 0x6e, 0x61,
                0x6e, 0x72, 0x61, 0x6d, 0x61, 0x00,
        };
        var small = new NBTCompound("hello world", new ArrayList<>() {{
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
