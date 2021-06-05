package com.pygostylia.osprey;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ArrowEntity extends ObjectEntity {
    int shooterId;
    boolean critical;
    ScheduledFuture<?> tick;

    public ArrowEntity(Entity shooter, Position position, Velocity velocity) {
        super(position, velocity);
        noCollision = true;
        shooterId = shooter.id;
        // Are arrows backwards or is Position backwards?
        position.pitch = -position.pitch;
        position.yaw = -position.yaw;
    }

    public ArrowEntity(Entity shooter, Position position, Duration pullTime) {
        this(shooter, position, Velocity.directionMagnitude(
                position,
                Math.min(30f, pullTime.toMillis() / 1000f * 30)
        ));
        if (pullTime.compareTo(Duration.ofSeconds(1)) > 0) {
            critical = true;
        }
    }

    @Override
    public void spawn() {
        super.spawn();
        tick = Main.entityController.submitForEachTick(this::stepPhysics);
        Main.entityController.submit(this::destroy, 30, TimeUnit.SECONDS);
        if (critical) {
            for (Player player : playersWithLoaded) {
                try {
                    player.sendEntityMetadata(this, 7, 0, (byte) 1);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        tick.cancel(false);
    }

    @Override
    int type() {
        return 2;
    }

    @Override
    float colliderXZ() {
        return 0.5f;
    }

    @Override
    float colliderY() {
        return 0.5f;
    }

    @Override
    int spawnData() {
        return shooterId + 1;
    }

    private boolean intersectingBlock() {
        try {
            return Main.world.block(location()) != 0;
        } catch (IOException ignored) {
            return true;
        }
    }

    private void stepPhysics() {
        velocity = new Velocity(velocity.x(), velocity.y() - 0.5f, velocity.z());
        double dx, dy, dz;
        dx = velocity.x() / 20;
        dy = velocity.y() / 20;
        dz = velocity.z() / 20;
        position.moveBy(dx, dy, dz);
        position.updateFacing(dx, dy, dz);
        for (Player player : playersWithLoaded) {
            try {
                player.sendEntityPositionAndRotation(id, dx, dy, dz, position);
                player.sendEntityVelocity(this, velocity);
            } catch (IOException ignored) {}
        }
        if (intersectingBlock()) {
            destroy();
        }
    }
}