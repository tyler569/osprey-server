package com.pygostylia.osprey;

import java.io.IOException;

abstract public class ObjectEntity extends Entity {
    Velocity velocity;

    public ObjectEntity(Position position, Velocity velocity) {
        super(position);
        this.velocity = velocity;
    }

    public void spawnForPlayer(Player player) throws IOException {
        super.spawnForPlayer(player);
        player.sendSpawnEntity(this);
    }
}