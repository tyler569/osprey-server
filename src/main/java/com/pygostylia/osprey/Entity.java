package com.pygostylia.osprey;

import java.io.IOException;
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
        id = Main.nextEntityId++;
        uuid = UUID.randomUUID();
        playersWithLoaded = new ArrayList<>();
        Main.addEntity(this);
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
        Main.removeEntity(this);
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
        if (position.x + ex < point.x || point.x < position.x - ex) return false;
        if (position.z + ex < point.z || point.z < position.z - ex) return false;
        return !(position.y + ey < point.y) && !(point.y <= position.y);
    }

    boolean collides(Location block) {
        return false;
    }

    boolean collides(Entity entity) {
        return false;
    }
}
