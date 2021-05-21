package io.philbrick.minecraft.nbt;

import java.io.*;
import java.util.*;

public class NBTTest {
    public static void main(String[] args) throws IOException {
        small();
        large();
    }

    public static void large() throws IOException {
        var byteArrayTest = new byte[1000];
        for (int i=0; i<1000; i++) {
            byteArrayTest[i] = (byte)((i * i * 255 + i * 7) % 100);
        }
        System.out.println(Arrays.toString(Arrays.copyOfRange(byteArrayTest, 0, 6)));

        var large = new NBTCompound("Level",
            new NBTLong("longTest", 9223372036854775807L),
            new NBTShort("shortTest", (short)32767),
            new NBTString("stringTest", "HELLO WORLD THIS IS A TEST STRING \305\304\326!"),
            new NBTFloat("floatTest", 0.49823147058486938f),
            new NBTInteger("intTest", 2147483647),
            new NBTCompound("nested compound test",
                new NBTCompound("ham",
                    new NBTString("name", "Hampus"),
                    new NBTFloat("value", 0.75f)
                ),
                new NBTCompound("egg",
                    new NBTString("name", "Eggbert"),
                    new NBTFloat("value", 0.5f)
                )
            ),
            new NBTList<>("listTest (long)",
                new NBTLong(null, 11),
                new NBTLong(null, 12),
                new NBTLong(null, 13),
                new NBTLong(null, 14),
                new NBTLong(null, 15)
            ),
            new NBTList<>("listTest (compound)",
                new NBTCompound(null,
                    new NBTString("name", "Compound tag #0"),
                    new NBTLong("created-on", 1264099775885L)
                ),
                new NBTCompound(null,
                    new NBTString("name", "Compound tag #1"),
                    new NBTLong("created-on", 1264099775885L)
                )
            ),
            new NBTByte("byteTest", (byte)127),
            new NBTByteArray("byteArrayTest (the first 1000 values of (n*n*255+n*7)%100, starting with n=0 (0, 62, 34, 16, 8, ...))",
                byteArrayTest),
            new NBTDouble("doubleTest", 0.49312871321823148)
        );

        var stream = new ByteArrayOutputStream();
        try {
            large.encode(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        var encoded = stream.toByteArray();
        byte[] expected;

        {
            var file = new File("src/io/philbrick/minecraft/nbt/bigtest.nbt");
            var fs = new FileInputStream(file);
            expected = fs.readAllBytes();
            fs.close();
        }

        System.out.println(Arrays.toString(encoded));
        System.out.println(Arrays.toString(expected));
        System.out.println(Arrays.equals(expected, encoded));
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
