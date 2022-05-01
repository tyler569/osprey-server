package com.pygostylia.osprey.entities;

import com.pygostylia.osprey.Position;
import com.pygostylia.osprey.Registry;

public class LightningEntity extends ObjectEntity {
    static final int TYPE = Registry.entity("minecraft:lightning_bolt");

    public LightningEntity(Position position) {
        super(position);
    }

    @Override
    int type() {
        return TYPE;
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
