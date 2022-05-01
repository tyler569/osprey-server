package com.pygostylia.osprey;

import java.time.Duration;

public class EntityPosition {
    public boolean onGround;
    public double x, y, z;
    public float pitch;
    public float yaw;

    public EntityPosition() {
        x = 0.5;
        z = 0.5;
        y = 32;
    }

    public EntityPosition(BlockPosition blockPosition) {
        x = blockPosition.x();
        y = blockPosition.y();
        z = blockPosition.z();
    }

    public EntityPosition(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public EntityPosition(EntityPosition entityPosition) {
        this.x = entityPosition.x;
        this.y = entityPosition.y;
        this.z = entityPosition.z;
        this.yaw = entityPosition.yaw;
        this.pitch = entityPosition.pitch;
        this.onGround = entityPosition.onGround;
    }

    public static EntityPosition middle(BlockPosition blockPosition) {
        EntityPosition p = new EntityPosition();
        p.x = blockPosition.x() + 0.5;
        p.y = blockPosition.y();
        p.z = blockPosition.z() + 0.5;
        return p;
    }

    public static EntityPosition orientation(float yaw, float pitch) {
        var position = new EntityPosition();
        position.yaw = yaw;
        position.pitch = pitch;
        return position;
    }

    public void moveTo(BlockPosition blockPosition) {
        x = blockPosition.x() + 0.5;
        y = blockPosition.y();
        z = blockPosition.z() + 0.5;
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

    public BlockPosition location() {
        return new BlockPosition(
                (int) Math.floor(x),
                (int) Math.floor(y),
                (int) Math.floor(z)
        );
    }

    public ChunkPosition chunkLocation() {
        return new ChunkPosition(chunkX(), chunkZ());
    }

    public String toString() {
        return String.format("EntityPosition[x=%f, y=%f, z=%f, pitch=%f, yaw=%f]", x, y, z, pitch, yaw);
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

    public EntityPosition offset(double dx, double dy, double dz) {
        EntityPosition p = new EntityPosition(this);
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
