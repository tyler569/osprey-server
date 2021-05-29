package io.philbrick.minecraft;

public record Location(int x, int y, int z) {
    Location(long protocolLocation) {
        this(
            (int)(protocolLocation >> 38),
            (int)(protocolLocation & 0xFFF),
            (int)((protocolLocation << 26) >> 38) // sign extension
        );
    }

    int chunkX() {
        return x >> 4;
    }

    int chunkZ() {
        return z >> 4;
    }

    ChunkLocation chunkLocation() {
        return new ChunkLocation(chunkX(), chunkZ());
    }

    Location offsetByChunks(int dx, int dz) {
        return new Location(x + dx * 16, y, z + dz * 16);
    }

    Location offset(int dx, int dy, int dz) {
        return new Location(x + dx, y + dy, z + dz);
    }

    Location offset(Direction direction, int delta) {
        return switch (direction) {
            case North -> new Location(x, y, z - delta);
            case South -> new Location(x, y, z + delta);
            case West -> new Location(x - delta, y, z);
            case East -> new Location(x + delta, y, z);
            case Up -> new Location(x, y + delta, z);
            case Down -> new Location(x, y - delta, z);
        };
    }

    Location positionInChunk() {
        return new Location(x & 0xF, y, z & 0xF);
    }

    long encode() {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    int blockIndex() {
        return y * 256 + z * 16 + x;
    }
}
