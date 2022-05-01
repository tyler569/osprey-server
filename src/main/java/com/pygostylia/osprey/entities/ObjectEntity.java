package com.pygostylia.osprey.entities;

import com.pygostylia.osprey.Main;
import com.pygostylia.osprey.Position;
import com.pygostylia.osprey.Velocity;

abstract public class ObjectEntity extends Entity {
    Velocity velocity;

    public ObjectEntity(Position position) {
        super(position);
        this.velocity = Velocity.zero();
    }

    public ObjectEntity(Position position, Velocity velocity) {
        super(position);
        this.velocity = velocity;
    }

    public void spawnForPlayer(Player player) {
        super.spawnForPlayer(player);
        player.sendSpawnEntity(this, spawnData());
    }

    public void spawn() {
        Main.playersWithin(64, position.location()).forEach(this::spawnForPlayer);
    }
}
