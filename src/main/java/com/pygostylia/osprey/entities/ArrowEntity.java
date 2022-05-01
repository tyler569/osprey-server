package com.pygostylia.osprey.entities;

import com.pygostylia.osprey.*;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ArrowEntity extends ObjectEntity {
    static final int TYPE = Registry.entity("minecraft:arrow");

    int shooterId;
    boolean critical;
    boolean stuck;
    boolean explode;
    boolean bulletTime;
    ScheduledFuture<?> tick;

    public ArrowEntity(Entity shooter, EntityPosition entityPosition, Velocity velocity) {
        super(entityPosition, velocity);
        noCollision = true;
        shooterId = shooter.id;
        // Are arrows backwards or is EntityPosition backwards?
        entityPosition.pitch = -entityPosition.pitch;
        entityPosition.yaw = -entityPosition.yaw;
    }

    public ArrowEntity(Entity shooter, EntityPosition entityPosition, Duration pullTime) {
        this(shooter, entityPosition, Velocity.directionMagnitude(
                entityPosition,
                Math.min(30f, pullTime.toMillis() / 1000f * 30)
        ));
        if (pullTime.compareTo(Duration.ofSeconds(1)) > 0) {
            critical = true;
        }
    }

    @Override
    public void spawn() {
        super.spawn();
        tick = Main.scheduler.submitForEachTick(this::stepPhysics);
        Main.scheduler.submit(this::destroy, 30, TimeUnit.SECONDS);
        if (critical) {
            for (Player player : playersWithLoaded) {
                player.sendEntityMetadata(this, 7, 0, (byte) 1);
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
        return TYPE;
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
        return Main.world.block(location()) != 0;
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
        entityPosition.moveBy(dx, dy, dz);
        if (!stuck) {
            entityPosition.updateFacing(dx, dy, dz);
        }
        for (Player player : playersWithLoaded) {
            player.sendEntityPositionAndRotation(id, dx, dy, dz, entityPosition);
            player.sendEntityVelocity(this, velocity.divide(10));
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
            Collection<BlockPosition> boomBlocks = Explosion.generateBoomBlocks(location(), 5.5f);
            for (BlockPosition boomBlock : boomBlocks) {
                Main.world.setBlock(boomBlock, 0);
            }
            playersWithLoaded.forEach(player -> player.sendExplosion(entityPosition, 5.5f, boomBlocks, Velocity.zero()));
        }
    }
}
