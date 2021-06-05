package com.pygostylia.osprey;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class FireworkEntity extends ObjectEntity {
    int ridingEntity;

    public FireworkEntity(Position position, Velocity velocity) {
        super(position, velocity);
    }

    @Override
    int type() {
        return 27;
    }

    @Override
    public void spawn() {
        super.spawn();
        Main.entityController.submit(this::destroy, 1, TimeUnit.SECONDS);
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
