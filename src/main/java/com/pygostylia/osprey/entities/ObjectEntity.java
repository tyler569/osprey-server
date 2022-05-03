package com.pygostylia.osprey.entities;

import com.pygostylia.osprey.EntityPosition;
import com.pygostylia.osprey.Main;
import com.pygostylia.osprey.Velocity;

abstract public class ObjectEntity extends Entity {
    public Velocity velocity;

    public ObjectEntity(EntityPosition entityPosition) {
        super(entityPosition);
        this.velocity = Velocity.zero();
    }

    public ObjectEntity(EntityPosition entityPosition, Velocity velocity) {
        super(entityPosition);
        this.velocity = velocity;
    }

    public void spawnForPlayer(Player player) {
        super.spawnForPlayer(player);
        player.sendSpawnEntity(this);
    }

    public void spawn() {
        Main.playersWithin(64, entityPosition.blockPosition()).forEach(this::spawnForPlayer);
    }
}
