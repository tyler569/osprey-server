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

    Location chunkAddress() {
        return new Location(chunkX(), 0, chunkZ());
    }

    Location offsetByChunks(int dx, int dz) {
        return new Location(x + dx * 16, y, z + dz * 16);
    }

    Location offset(int dx, int dy, int dz) {
        return new Location(x + dx, y + dy, z + dz);
    }

    Location positionInChunk() {
        return new Location(x & 0xF, y, z & 0xF);
    }

    long encode() {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
