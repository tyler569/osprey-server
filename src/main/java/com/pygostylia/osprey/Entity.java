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
    }

    Entity(Position position) {
        this();
        this.position = position;
    }

    abstract int type();

    void spawnForPlayer(Player player) throws IOException {
        playersWithLoaded.add(player);
    }
}
