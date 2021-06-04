package com.pygostylia.osprey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class VelocityTest {
    static Stream<Arguments> testCases() {
        final float sin45 = (float) Math.sin(Math.toRadians(45));

        return Stream.of(
                Arguments.of(Position.orientation(0, 0), new Velocity(0, 0, 400)),
                Arguments.of(Position.orientation(90, 0), new Velocity(-400, 0, 0)),
                Arguments.of(Position.orientation(180, 0), new Velocity(0, 0, -400)),
                Arguments.of(Position.orientation(270, 0), new Velocity(400, 0, 0)),
                Arguments.of(Position.orientation(0, -90), new Velocity(0, 400, 0)),
                Arguments.of(Position.orientation(0, 90), new Velocity(0, -400, 0)),
                Arguments.of(Position.orientation(45, 0), new Velocity(-400 * sin45, 0, 400 * sin45)),
                Arguments.of(Position.orientation(45, 45), new Velocity(-200, -400 * sin45, 200))
        );
    }

    void assertVelocityClose(Velocity actual, Velocity expected) {
        float epsilon = 0.0005f;
        if (Math.abs(actual.x() - expected.x()) > epsilon ||
            Math.abs(actual.y() - expected.y()) > epsilon ||
            Math.abs(actual.z() - expected.z()) > epsilon) {

            fail(String.format("""
                
                Expected: %s
                Actual:   %s
                """, expected, actual)
            );
        }
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void directionMagnitude(Position position, Velocity expected) {
        assertVelocityClose(Velocity.directionMagnitude(position, 1), expected);
    }
}