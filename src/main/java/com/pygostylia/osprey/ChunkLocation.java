package com.pygostylia.osprey;

public record ChunkLocation(int x, int z) {
    public Double distanceFrom(ChunkLocation location) {
        return Math.sqrt(
            Math.pow(Math.abs(location.x - x), 2) +
            Math.pow(Math.abs(location.z - z), 2)
        );
    }
}
