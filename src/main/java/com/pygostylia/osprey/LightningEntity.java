package com.pygostylia.osprey;

public class LightningEntity extends ObjectEntity {
    public LightningEntity(Position position) {
        super(position);
    }

    @Override
    int type() {
        return 41;
    }

    @Override
    float colliderXZ() {
        return 0;
    }

    @Override
    float colliderY() {
        return 0;
    }
}
