package com.pygostylia.osprey;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class FireworkEntity extends ObjectEntity {
    int ridingEntity;

    public FireworkEntity(Position position, Velocity velocity) {
        super(position, velocity);
        noCollision = true;
    }

    @Override
    int type() {
        return 27;
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
        Main.scheduler.submit(this::destroy, 1, TimeUnit.SECONDS);
    }

    public void spawnWithRider(int ridingEntity) {
        spawn();
        this.ridingEntity = ridingEntity;
        playersWithLoaded.forEach((player) -> {
            try {
                player.sendEntityMetadata(this, 8, 17, ridingEntity);
            } catch (IOException e) {
                player.println("Unable to send entity metadata to <-");
                e.printStackTrace();
            }
        });
    }
}
