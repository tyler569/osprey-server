package com.pygostylia.osprey;

import java.util.concurrent.TimeUnit;

public class FireworkEntity extends ObjectEntity {
    static final int TYPE = Registry.entity("minecraft:firework_rocket");

    int ridingEntity;

    public FireworkEntity(EntityPosition entityPosition, Velocity velocity) {
        super(entityPosition, velocity);
        noCollision = true;
    }

    @Override
    public int type() {
        return TYPE;
    }

    @Override
    float colliderXZ() {
        return 0.25f;
    }

    @Override
    float colliderY() {
        return 0.25f;
    }

    @Override
    public void spawn() {
        super.spawn();
        Main.INSTANCE.getScheduler().submit(this::destroy, 1, TimeUnit.SECONDS);
    }

    public void spawnWithRider(int ridingEntity) {
        spawn();
        this.ridingEntity = ridingEntity;
        playersWithLoaded.forEach(player -> player.sendEntityMetadata(this, 8, 17, ridingEntity));
    }
}
