package io.philbrick.minecraft;

public class Position {
    boolean onGround;
    double x, y, z;
    float pitch;
    float yaw;

    Position() {
        x = 0.5;
        z = 0.5;
        y = 32;
    }

    Position(Location p) {
        x = p.x() + 0.5;
        y = p.y();
        z = p.z() + 0.5;
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

    ChunkLocation chunkLocation() {
        return new ChunkLocation(chunkX(), chunkZ());
    }

    public String toString() {
        return String.format("Position{%f, %f, %f, %f, %f}", x, y, z, pitch, yaw);
    }
}
