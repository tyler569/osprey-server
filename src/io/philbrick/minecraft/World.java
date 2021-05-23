package io.philbrick.minecraft;

import java.util.*;

public class World {
    HashMap<Location, Chunk> chunks;

    World() {
        chunks = new HashMap<>();
    }

    Chunk findOrCreateChunk(Location location) {
        return null;
    }
}
