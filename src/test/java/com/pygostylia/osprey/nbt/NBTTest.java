package com.pygostylia.osprey.nbt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class NBTTest {
    @Test
    public void large() throws IOException {
        var byteArrayTest = new byte[1000];
        for (int i=0; i<1000; i++) {
            byteArrayTest[i] = (byte)((i * i * 255 + i * 7) % 100);
        }
        Assertions.assertArrayEquals(
                Arrays.copyOfRange(byteArrayTest, 0, 5),
                new byte[] {0, 62, 34, 16, 8}
        );

        var large = new NBTCompound("Level");
        large.put("longTest", 9223372036854775807L);
        large.put("shortTest", (short) 32767);
        large.put("stringTest", "HELLO WORLD THIS IS A TEST STRING \305\304\326!");
        large.put("floatTest", 0.49823147058486938f);
        large.put("intTest", 2147483647);

        var nested = new NBTCompound();

        var ham = new NBTCompound();
        ham.put("name", "Hampus");
        ham.put("value", 0.75f);

        var egg = new NBTCompound();
        egg.put("name", "Eggbert");
        egg.put("value", 0.5f);

        nested.put("ham", ham);
        nested.put("egg", egg);

        large.put("nested compound test", nested);

        var listTest = new NBTList<NBTLong>();
        listTest.add(new NBTLong(11L));
        listTest.add(new NBTLong(12L));
        listTest.add(new NBTLong(13L));
        listTest.add(new NBTLong(14L));
        listTest.add(new NBTLong(15L));

        large.put("listTest (long)", listTest);

        var listTest2 = new NBTList<NBTCompound>();

        var compound0 = new NBTCompound();
        compound0.put("name", "Compound tag #0");
        compound0.put("created-on", 1264099775885L);

        var compound1 = new NBTCompound();
        compound1.put("name", "Compound tag #1");
        compound1.put("created-on", 1264099775885L);

        listTest2.add(compound0);
        listTest2.add(compound1);

        large.put("listTest (compound)", listTest2);

        large.put("byteTest", (byte) 127);
        large.put("byteArrayTest (the first 1000 values of (n*n*255+n*7)%100, starting with n=0 (0, 62, 34, 16, 8, ...))", byteArrayTest);
        large.put("doubleTest", 0.49312871321823148);

        var stream = new ByteArrayOutputStream();
        large.write(stream);

        var encoded = stream.toByteArray();
        byte[] expected;

        {
            var file = new File("src/main/java/com/pygostylia/osprey/nbt/bigtest.nbt");
            var fs = new FileInputStream(file);
            expected = fs.readAllBytes();
            fs.close();
        }

        Assertions.assertArrayEquals(encoded, expected);
    }

    @Test
    public void small() {
        var expected = new byte[]{
            0x0a, 0x00, 0x0b, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x20,
            0x77, 0x6f, 0x72, 0x6c, 0x64, 0x08, 0x00, 0x04, 0x6e,
            0x61, 0x6d, 0x65, 0x00, 0x09, 0x42, 0x61, 0x6e, 0x61,
            0x6e, 0x72, 0x61, 0x6d, 0x61, 0x00,
        };
        // var small = new NBTCompound("hello world", new ArrayList<>() {{
        //     add(new NBTString("name", "Bananrama"));
        // }});
        var small = new NBTCompound("hello world");
        small.put("name", "Bananrama");

        var stream = new ByteArrayOutputStream();
        try {
            small.write(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        var encoded = stream.toByteArray();
        Assertions.assertArrayEquals(encoded, expected);
    }
}
