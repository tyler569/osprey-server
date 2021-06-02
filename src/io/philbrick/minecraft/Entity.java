package io.philbrick.minecraft;

import java.util.*;

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
