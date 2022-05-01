package com.pygostylia.osprey.entities;

import com.pygostylia.osprey.EntityPosition;
import com.pygostylia.osprey.Main;
import com.pygostylia.osprey.Registry;

import java.util.concurrent.TimeUnit;

public class FallingBlockEntity extends ObjectEntity {
    static final int TYPE = Registry.entity("minecraft:falling_block");

    int blockType;

    public FallingBlockEntity(EntityPosition entityPosition, int blockType) {
        super(entityPosition);
        this.blockType = blockType;
    }

    @Override
    int type() {
        return TYPE;
    }

    @Override
    float colliderXZ() {
        return 0.98f;
    }

    @Override
    float colliderY() {
        return 0.98f;
    }

    @Override
    public void spawn() {
        super.spawn();
        Main.scheduler.submit(this::destroy, 30, TimeUnit.SECONDS);
    }

    @Override
    public int spawnData() {
        return blockType;
    }
}
