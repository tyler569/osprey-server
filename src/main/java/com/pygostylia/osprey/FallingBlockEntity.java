package com.pygostylia.osprey;

import java.util.concurrent.TimeUnit;

public class FallingBlockEntity extends ObjectEntity {
    int blockType;

    public FallingBlockEntity(Position position, int blockType) {
        super(position);
        this.blockType = blockType;
    }

    @Override
    int type() {
        return 26;
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
