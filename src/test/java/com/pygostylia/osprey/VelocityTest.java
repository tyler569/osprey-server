package com.pygostylia.osprey;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

class VelocityTest {
    static Stream<Arguments> testCases() {
        final float sin45 = (float) Math.sin(Math.toRadians(45));

        return Stream.of(
                Arguments.of(EntityPosition.orientation(0, 0), new Velocity(0, 0, 1)),
                Arguments.of(EntityPosition.orientation(90, 0), new Velocity(-1, 0, 0)),
                Arguments.of(EntityPosition.orientation(180, 0), new Velocity(0, 0, -1)),
                Arguments.of(EntityPosition.orientation(270, 0), new Velocity(1, 0, 0)),
                Arguments.of(EntityPosition.orientation(0, -90), new Velocity(0, 1, 0)),
                Arguments.of(EntityPosition.orientation(0, 90), new Velocity(0, -1, 0)),
                Arguments.of(EntityPosition.orientation(45, 0), new Velocity(-sin45, 0, sin45)),
                Arguments.of(EntityPosition.orientation(45, 45), new Velocity(-0.5f, -sin45, 0.5f))
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
    void directionMagnitude(EntityPosition entityPosition, Velocity expected) {
        assertVelocityClose(Velocity.directionMagnitude(entityPosition, 1), expected);
    }
}