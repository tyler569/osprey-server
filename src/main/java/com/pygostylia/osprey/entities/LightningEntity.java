package com.pygostylia.osprey.entities;

import com.pygostylia.osprey.EntityPosition;
import com.pygostylia.osprey.Registry;

public class LightningEntity extends ObjectEntity {
    static final int TYPE = Registry.entity("minecraft:lightning_bolt");

    public LightningEntity(EntityPosition entityPosition) {
        super(entityPosition);
    }

    @Override
    public int type() {
        return TYPE;
    }

    @Override
    public float colliderXZ() {
        return 0;
    }

    @Override
    public float colliderY() {
        return 0;
    }
}
