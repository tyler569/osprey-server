package com.pygostylia.osprey;

public record BlockPosition(int x, int y, int z) {
    BlockPosition(long protocolLocation) {
        this((int) (protocolLocation >> 38), (int) (protocolLocation & 0xFFF), (int) ((protocolLocation << 26) >> 38) /* sign extension */);
    }

    private static int parseRelative(int current, String str) {
        if (str.startsWith("~")) {
            if (str.length() == 1) {
                return current;
            } else {
                return (int) (current + Float.parseFloat(str.substring(1)));
            }
        } else {
            return (int) Float.parseFloat(str);
        }
    }

    public static BlockPosition relativeLocation(BlockPosition base, String[] args) {
        int x, y, z;
        x = parseRelative(base.x(), args[0]);
        y = parseRelative(base.y(), args[1]);
        z = parseRelative(base.z(), args[2]);
        return new BlockPosition(x, y, z);
    }

    int chunkX() {
        return x >> 4;
    }

    int chunkZ() {
        return z >> 4;
    }

    ChunkPosition chunkLocation() {
        return new ChunkPosition(chunkX(), chunkZ());
    }

    BlockPosition offsetByChunks(int dx, int dz) {
        return new BlockPosition(x + dx * 16, y, z + dz * 16);
    }

    public BlockPosition offset(int dx, int dy, int dz) {
        return new BlockPosition(x + dx, y + dy, z + dz);
    }

    BlockPosition offset(Direction direction, int delta) {
        return switch (direction) {
            case North -> new BlockPosition(x, y, z - delta);
            case South -> new BlockPosition(x, y, z + delta);
            case West -> new BlockPosition(x - delta, y, z);
            case East -> new BlockPosition(x + delta, y, z);
            case Up -> new BlockPosition(x, y + delta, z);
            case Down -> new BlockPosition(x, y - delta, z);
        };
    }

    BlockPosition positionInChunk() {
        return new BlockPosition(x & 0xF, y, z & 0xF);
    }

    public long encode() {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    int blockIndex() {
        return y * 256 + z * 16 + x;
    }

    boolean withinRadiusOf(int radius, BlockPosition blockPosition) {
        return Math.abs(blockPosition.x - x) < radius && Math.abs(blockPosition.z - z) < radius;
    }

    double distance(BlockPosition other) {
        return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
    }
}
