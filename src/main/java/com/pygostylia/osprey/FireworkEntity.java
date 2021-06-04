package com.pygostylia.osprey;

public class FireworkEntity extends ObjectEntity {
    public FireworkEntity(Position position, Velocity velocity) {
        super(position, velocity);
    }

    @Override
    int type() {
        return 27;
    }
}
