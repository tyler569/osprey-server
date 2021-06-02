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

    Position(Location location) {
        x = location.x();
        y = location.y();
        z = location.z();
    }

    static Position middle(Location location) {
        Position p = new Position();
        p.x = location.x() + 0.5;
        p.y = location.y();
        p.z = location.z() + 0.5;
        return p;
    }

    void moveTo(Location location) {
        x = location.x() + 0.5;
        y = location.y();
        z = location.z() + 0.5;
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
        return new Location(
            (int)Math.floor(x),
            (int)Math.floor(y),
            (int)Math.floor(z)
        );
    }

    ChunkLocation chunkLocation() {
        return new ChunkLocation(chunkX(), chunkZ());
    }

    public String toString() {
        return String.format("Position{%f, %f, %f, %f, %f}", x, y, z, pitch, yaw);
    }
}
