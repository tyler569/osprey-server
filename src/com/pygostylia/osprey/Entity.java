package com.pygostylia.osprey;

import java.util.ArrayList;
import java.util.UUID;

abstract public class Entity {
    int entityType;
    int entityId;
    UUID uuid;
    Position position;

    ArrayList<Player> playersWithLoaded;

    Entity(int entityType) {
        entityId = Main.nextEntityId++;
    }

    abstract void update();
}
