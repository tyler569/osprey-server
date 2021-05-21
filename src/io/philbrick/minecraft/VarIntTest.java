package io.philbrick.minecraft;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class VarIntTest {
    @Test
    void encode() {
        assertArrayEquals(VarInt.encode(0), new byte[]{0});
        assertArrayEquals(VarInt.encode(1), new byte[]{1});
        assertArrayEquals(VarInt.encode(2), new byte[]{2});
        assertArrayEquals(VarInt.encode(127), new byte[]{127});
        assertArrayEquals(VarInt.encode(128), new byte[]{-128, 1});
        assertArrayEquals(VarInt.encode(255), new byte[]{-1, 1});
        assertArrayEquals(VarInt.encode(2097151), new byte[]{-1, -1, 127});
        assertArrayEquals(VarInt.encode(2147483647), new byte[]{-1, -1, -1, -1, 7});
        assertArrayEquals(VarInt.encode(-1), new byte[]{-1, -1, -1, -1, 15});
        assertArrayEquals(VarInt.encode(-2147483648), new byte[]{-128, -128, -128, -128, 8});
    }
}