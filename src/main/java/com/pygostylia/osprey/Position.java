package com.pygostylia.osprey;

import java.time.Duration;

public class Position {
    public boolean onGround;
    public double x, y, z;
    public float pitch;
    public float yaw;

    public Position() {
        x = 0.5;
        z = 0.5;
        y = 32;
    }

    public Position(Location location) {
        x = location.x();
        y = location.y();
        z = location.z();
    }

    public Position(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Position(Position position) {
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
        this.yaw = position.yaw;
        this.pitch = position.pitch;
        this.onGround = position.onGround;
    }

    public static Position middle(Location location) {
        Position p = new Position();
        p.x = location.x() + 0.5;
        p.y = location.y();
        p.z = location.z() + 0.5;
        return p;
    }

    public static Position orientation(float yaw, float pitch) {
        var position = new Position();
        position.yaw = yaw;
        position.pitch = pitch;
        return position;
    }

    public void moveTo(Location location) {
        x = location.x() + 0.5;
        y = location.y();
        z = location.z() + 0.5;
    }

    public byte yawAngle() {
        return (byte) (yaw / 360 * 256);
    }

    public byte pitchAngle() {
        return (byte) (pitch / 360 * 256);
    }

    public int chunkX() {
        return (int) x >> 4;
    }

    public int chunkZ() {
        return (int) z >> 4;
    }

    public Location location() {
        return new Location(
                (int) Math.floor(x),
                (int) Math.floor(y),
                (int) Math.floor(z)
        );
    }

    public ChunkLocation chunkLocation() {
        return new ChunkLocation(chunkX(), chunkZ());
    }

    public String toString() {
        return String.format("Position[x=%f, y=%f, z=%f, pitch=%f, yaw=%f]", x, y, z, pitch, yaw);
    }

    public float pitchRadians() {
        return (float) Math.toRadians(pitch);
    }

    public float yawRadians() {
        return (float) Math.toRadians(yaw);
    }

    public void moveBy(double dx, double dy, double dz) {
        x += dx;
        y += dy;
        z += dz;
    }

    public Position offset(double dx, double dy, double dz) {
        Position p = new Position(this);
        p.moveBy(dx, dy, dz);
        return p;
    }

    public void updateFacing(double dx, double dy, double dz) {
        yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        pitch = (float) Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
    }

    public void stepVelocity(Velocity velocity, Duration timeStep) {
        double factor = (double) timeStep.toNanos() / 1_000_000_000;
        x += velocity.x() / factor;
        y += velocity.y() / factor;
        z += velocity.z() / factor;
    }
}
