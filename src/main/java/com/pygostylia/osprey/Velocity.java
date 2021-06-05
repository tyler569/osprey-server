package com.pygostylia.osprey;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

/*
The protocol representation of velocity is as a triple of Shorts,
representing velocity along the three axes in units of
1/8000 block / 50ms
 */
public record Velocity(float x, float y, float z) {
    public static Velocity zero() {
        return new Velocity(0, 0, 0);
    }

    void write(PacketBuilder os) throws IOException {
        os.writeShort((short) x);
        os.writeShort((short) y);
        os.writeShort((short) z);
    }

    static Velocity directionMagnitude(Position position, float speed) {
        var x = -Math.sin(position.yawRadians());
        var z = Math.cos(position.yawRadians());
        var yH = -Math.sin(position.pitchRadians());
        var yXZ = Math.cos(position.pitchRadians());
        x *= yXZ;
        z *= yXZ;
        return new Velocity(
                blockPerSecondToProtocol((float) x * speed),
                blockPerSecondToProtocol((float) yH * speed),
                blockPerSecondToProtocol((float) z * speed)
        );
    }

    static float protocolToBlockPerSecond(float protocol) {
        return protocol / 400f;
    }

    static float blockPerSecondToProtocol(float blockPerSecond) {
        return blockPerSecond * 400f;
    }
}
