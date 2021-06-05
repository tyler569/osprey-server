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

    void spawnForPlayer(Player player) throws IOException {
        playersWithLoaded.add(player);
    }

    void destroyForPlayer(Player player) throws IOException {
        player.sendDestroyEntity(this);
        playersWithLoaded.remove(player);
    }

    void destroy() {
        playersWithLoaded.forEach((player) -> {
            try {
                player.sendDestroyEntity(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        playersWithLoaded.clear();
        Main.removeEntity(this);
    }

    Location location() {
        return position.location();
    }

    public int id() {
        return id;
    };
}
