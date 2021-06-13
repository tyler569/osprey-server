package com.pygostylia.osprey;

import java.io.IOException;

/*
The protocol representation of velocity is as a triple of Shorts,
representing velocity along the three axes in units of
1/8000 block / 50ms
 */
public record Velocity(float x, float y, float z) {
    public static Velocity zero() {
        return new Velocity(0, 0, 0);
    }

    public void write(PacketBuilder os) throws IOException {
        os.writeShort(blockPerSecondToProtocol(x));
        os.writeShort(blockPerSecondToProtocol(y));
        os.writeShort(blockPerSecondToProtocol(z));
    }

    public static Velocity directionMagnitude(Position position, float speed) {
        var x = -Math.sin(position.yawRadians());
        var z = Math.cos(position.yawRadians());
        var yH = -Math.sin(position.pitchRadians());
        var yXZ = Math.cos(position.pitchRadians());
        x *= yXZ;
        z *= yXZ;
        return new Velocity(
                (float) x * speed,
                (float) yH * speed,
                (float) z * speed
        );
    }

    public static float protocolToBlockPerSecond(short protocol) {
        return protocol / 400f;
    }

    public static short blockPerSecondToProtocol(float blockPerSecond) {
        return (short) (blockPerSecond * 400f);
    }

    public Velocity divide(float dividend) {
        return new Velocity(x / dividend, y / dividend, z / dividend);
    }
}
