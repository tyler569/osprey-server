package com.pygostylia.osprey;

public record ChunkPosition(int x, int z) {
    public Double distanceFrom(ChunkPosition location) {
        return Math.sqrt(
                Math.pow(Math.abs(location.x - x), 2) +
                        Math.pow(Math.abs(location.z - z), 2)
        );
    }
}
