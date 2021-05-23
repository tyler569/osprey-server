package io.philbrick.minecraft;

public class Position {
    boolean onGround;
    double x, y, z;
    float pitch;
    float yaw;

    Position() {
        x = 0;
        y = 32;
        z = 0;
        pitch = 0;
        yaw = 0;
    }

    Position(Location p) {
        x = p.x();
        y = p.y();
        z = p.z();
        pitch = 0;
        yaw = 0;
    }

    byte yawAngle() {
        return (byte)(yaw / 360 * 256);
    }

    byte pitchAngle() {
        return (byte)(pitch / 360 * 256);
    }

    int chunkX() {
        return (int)x >> 4;
    }

    int chunkZ() {
        return (int)z >> 4;
    }

    Location location() {
        return new Location((int)x, (int)y, (int)z);
    }
}
