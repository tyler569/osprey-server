package com.pygostylia.osprey;

public class ItemEntity extends ObjectEntity {
    static int TYPE = Registry.entity("minecraft:item");

    Slot itemInfo;

    public ItemEntity(Position position, Velocity velocity, Slot itemInfo) {
        super(position, velocity);
        this.itemInfo = itemInfo;
    }

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

    @Override
    public void spawn() {
        super.spawn();
        playersWithLoaded.forEach(player -> player.sendEntityMetadata(this, 7, 6, itemInfo));
        playersWithLoaded.forEach(player -> player.sendEntityVelocity(this, velocity));
    }
}
