package io.philbrick.minecraft;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.io.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

class VarIntTest {
    static Stream<Arguments> testCases() {
        return Stream.of(
            Arguments.of(0, new byte[]{0}),
            Arguments.of(0, new byte[]{0}),
            Arguments.of(1, new byte[]{1}),
            Arguments.of(2, new byte[]{2}),
            Arguments.of(127, new byte[]{127}),
            Arguments.of(128, new byte[]{-128, 1}),
            Arguments.of(255, new byte[]{-1, 1}),
            Arguments.of(2097151, new byte[]{-1, -1, 127}),
            Arguments.of(2147483647, new byte[]{-1, -1, -1, -1, 7}),
            Arguments.of(-1, new byte[]{-1, -1, -1, -1, 15}),
            Arguments.of(-2147483648, new byte[]{-128, -128, -128, -128, 8})
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void encode(int number, byte[] encoded) {
        assertArrayEquals(VarInt.encode(number), encoded);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void decode(int number, byte[] encoded) {
        var is = new ByteArrayInputStream(encoded);
        try {
            assertEquals(VarInt.read(is), number);
        } catch (IOException e) {
            fail("Unexpected Exception");
        }
    }
}