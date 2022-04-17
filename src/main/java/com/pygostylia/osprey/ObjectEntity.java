package com.pygostylia.osprey;

abstract public class ObjectEntity extends Entity {
    Velocity velocity;

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
        player.sendSpawnEntity(this, spawnData());
    }

    public void spawn() {
        Main.INSTANCE.playersWithin(64, entityPosition.blockPosition()).forEach(this::spawnForPlayer);
    }
}
