package com.pygostylia.osprey;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

abstract public class Entity {
    int id;
    UUID uuid;
    Position position;
    List<Player> playersWithLoaded;
    boolean noCollision;

    Entity() {
        uuid = UUID.randomUUID();
        playersWithLoaded = new ArrayList<>();
        id = Main.INSTANCE.addEntity(this);
    }

    Entity(Position position) {
        this();
        this.position = position;
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

    public Position position() {
        return position;
    }

    public Location location() {
        return position.location();
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

    boolean collides(Position point) {
        float ex = colliderXZ() / 2;
        float ey = colliderY();
        if (position.getX() + ex < point.getX() || point.getX() < position.getX() - ex) return false;
        if (position.getZ() + ex < point.getZ() || point.getZ() < position.getZ() - ex) return false;
        return !(position.getY() + ey < point.getY()) && !(point.getY() <= position.getY());
    }

    boolean collides(Location block) {
        return false;
    }

    boolean collides(Entity entity) {
        return false;
    }
}
