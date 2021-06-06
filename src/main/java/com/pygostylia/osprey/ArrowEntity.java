package com.pygostylia.osprey;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ArrowEntity extends ObjectEntity {
    int shooterId;
    boolean critical;
    boolean stuck;
    boolean explode;
    boolean bulletTime;
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
        float gravity = -0.5f;
        double tick = 20f;
        if (bulletTime) {
            gravity /= 10;
            tick *= 10;
        }

        if (!stuck) {
            velocity = new Velocity(velocity.x(), velocity.y() + gravity, velocity.z());
        }
        double dx, dy, dz;
        dx = velocity.x() / tick;
        dy = velocity.y() / tick;
        dz = velocity.z() / tick;
        position.moveBy(dx, dy, dz);
        if (!stuck) {
            position.updateFacing(dx, dy, dz);
        }
        for (Player player : playersWithLoaded) {
            try {
                player.sendEntityPositionAndRotation(id, dx, dy, dz, position);
                player.sendEntityVelocity(this, velocity.divide(10));
            } catch (IOException ignored) {
            }
        }
        if (intersectingBlock()) {
            stickInBlock();
        }
    }

    private void stickInBlock() {
        velocity = Velocity.zero();
        stuck = true;
        tick.cancel(false);
        if (explode) {
            Collection<Location> boomBlocks = Explosion.generateBoomBlocks(location(), 5.5f);
            for (Location boomBlock : boomBlocks) {
                try {
                    Main.world.setBlock(boomBlock, 0);
                } catch (IOException ignored) {
                }
            }
            playersWithLoaded.forEach(player -> {
                try {
                    player.sendExplosion(position, 5.5f, boomBlocks, Velocity.zero());
                } catch (IOException ignored) {
                }
            });
        }
    }
}
