package com.pygostylia.osprey.entities;

import com.pygostylia.osprey.BlockPosition;
import com.pygostylia.osprey.EntityPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

abstract public class Entity {
    int id;
    UUID uuid;
    public EntityPosition entityPosition;
    List<Player> playersWithLoaded;
    boolean noCollision;

    private static ConcurrentHashMap<Integer, Entity> all = new ConcurrentHashMap<>();

    Entity() {
        uuid = UUID.randomUUID();
        playersWithLoaded = new ArrayList<>();
        all.put(id, this);
    }

    Entity(EntityPosition entityPosition) {
        this();
        this.entityPosition = entityPosition;
    }

    public static Optional<Entity> byId(int id) {
        return Optional.ofNullable(all.get(id));
    }

    abstract public int type();

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
        all.remove(id);
    }

    public UUID uuid() {
        return uuid;
    }

    public EntityPosition position() {
        return entityPosition;
    }

    public BlockPosition blockPosition() {
        return entityPosition.blockPosition();
    }

    public int id() {
        return id;
    }

    public void interact(Player sender) {
    }

    public void attack(Player sender) {
    }

    public int spawnData() {
        return 0;
    }

    abstract public float colliderXZ();

    abstract public float colliderY();

    boolean collides(EntityPosition point) {
        float ex = colliderXZ() / 2;
        float ey = colliderY();
        if (entityPosition.x + ex < point.x || point.x < entityPosition.x - ex) return false;
        if (entityPosition.z + ex < point.z || point.z < entityPosition.z - ex) return false;
        return !(entityPosition.y + ey < point.y) && !(point.y <= entityPosition.y);
    }

    boolean collides(BlockPosition block) {
        return false;
    }

    boolean collides(Entity entity) {
        return false;
    }
}
