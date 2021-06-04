package com.pygostylia.osprey;

import java.util.concurrent.TimeUnit;

public class FireworkEntity extends ObjectEntity {
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
}
