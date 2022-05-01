package com.pygostylia.osprey.entities;

import com.pygostylia.osprey.EntityPosition;
import com.pygostylia.osprey.Registry;
import com.pygostylia.osprey.Slot;
import com.pygostylia.osprey.Velocity;

public class ItemEntity extends ObjectEntity {
    static final int TYPE = Registry.entity("minecraft:item");

    Slot itemInfo;

    public ItemEntity(EntityPosition entityPosition, Velocity velocity, Slot itemInfo) {
        super(entityPosition, velocity);
        this.itemInfo = itemInfo;
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

    @Override
    public void spawn() {
        super.spawn();
        playersWithLoaded.forEach(player -> player.sendEntityMetadata(this, 7, 6, itemInfo));
        playersWithLoaded.forEach(player -> player.sendEntityVelocity(this, velocity));
    }
}
