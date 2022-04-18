package com.pygostylia.osprey;

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
    float colliderXZ() {
        return 0;
    }

    @Override
    float colliderY() {
        return 0;
    }
}
