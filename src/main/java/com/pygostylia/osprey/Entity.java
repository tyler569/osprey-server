package com.pygostylia.osprey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

abstract public class Entity {
    int id;
    UUID uuid;
    EntityPosition entityPosition;
    List<Player> playersWithLoaded;
    boolean noCollision;

    Entity() {
        uuid = UUID.randomUUID();
        playersWithLoaded = new ArrayList<>();
        id = Main.INSTANCE.addEntity(this);
    }

    Entity(EntityPosition entityPosition) {
        this();
        this.entityPosition = entityPosition;
    }

    abstract int type();

    void spawnForPlayer(Player player) {
        playersWithLoaded.add(player);
    }

    void destroyForPlayer(Player player) {
        player.sendDestroyEntity(this);
        playersWithLoaded.remove(player);
    }

    public void destroy() {
        playersWithLoaded.forEach(player -> player.sendDestroyEntity(this));
        playersWithLoaded.clear();
        Main.INSTANCE.removeEntity(this);
    }

    public UUID uuid() {
        return uuid;
    }

    public EntityPosition position() {
        return entityPosition;
    }

    public BlockPosition location() {
        return entityPosition.blockPosition();
    }

    public int id() {
        return id;
    }

    public void interact(Player sender) {
    }

    public void attack(Player sender) {
    }

    int spawnData() {
        return 0;
    }

    abstract float colliderXZ();

    abstract float colliderY();

    boolean collides(EntityPosition point) {
        float ex = colliderXZ() / 2;
        float ey = colliderY();
        if (entityPosition.getX() + ex < point.getX() || point.getX() < entityPosition.getX() - ex) return false;
        if (entityPosition.getZ() + ex < point.getZ() || point.getZ() < entityPosition.getZ() - ex) return false;
        return !(entityPosition.getY() + ey < point.getY()) && !(point.getY() <= entityPosition.getY());
    }

    boolean collides(BlockPosition block) {
        return false;
    }

    boolean collides(Entity entity) {
        return false;
    }
}
