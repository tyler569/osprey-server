package com.pygostylia.osprey;

import java.io.IOException;

/*
The protocol representation of velocity is as a triple of Shorts,
representing velocity along the three axes in units of
1/8000 block / 50ms

Internally, I represent velocity as a float blocks / second, and
convert on-demand when encoding to the protocol layer.
 */
public record Velocity(float x, float y, float z) {
    public static Velocity zero() {
        return new Velocity(0, 0, 0);
    }

    public void write(MinecraftOutputStream os) throws IOException {
        os.writeShort(blockPerSecondToProtocol(x));
        os.writeShort(blockPerSecondToProtocol(y));
        os.writeShort(blockPerSecondToProtocol(z));
    }

    public static Velocity directionMagnitude(EntityPosition entityPosition, float speed) {
        var x = -Math.sin(entityPosition.yawRadians());
        var z = Math.cos(entityPosition.yawRadians());
        var yH = -Math.sin(entityPosition.pitchRadians());
        var yXZ = Math.cos(entityPosition.pitchRadians());
        x *= yXZ;
        z *= yXZ;
        return new Velocity(
                (float) x * speed,
                (float) yH * speed,
                (float) z * speed
        );
    }

    private static float protocolToBlockPerSecond(short protocol) {
        return protocol / 400f;
    }

    private static short blockPerSecondToProtocol(float blockPerSecond) {
        return (short) (blockPerSecond * 400f);
    }

    public Velocity divide(float dividend) {
        return new Velocity(x / dividend, y / dividend, z / dividend);
    }

    public Velocity add(float x, float y, float z) {
        return new Velocity(this.x + x, this.y + y, this.z + z);
    }

    public Velocity offsetGravity(float y) {
        return this.add(0, y, 0);
    }
}
