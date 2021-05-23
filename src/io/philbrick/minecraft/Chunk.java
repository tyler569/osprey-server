package io.philbrick.minecraft;

import java.io.*;
import java.util.*;

public class Chunk {
    Map<Location, Short> blockMap;
    short[] blockArray;

    Chunk() {
        blockMap = new HashMap<>();
        blockArray = new short[4096 * 16];
        for (int x = 0; x < 16; x += 1) {
            for (int z = 0; z < 16; z += 1) {
                setBlock(new Location(x, 0, z), 1);
                setBlock(new Location(x, 1, z), 1);
                if (x == z) setBlock(new Location(x, 2, z), 1);
                if (x == 16-z) setBlock(new Location(x, 10, z), 1);
            }
        }
    }

    static int index(Location location) {
        return location.y() * 256 + location.z() * 16 + location.x();
    }

    void setBlock(Location location, int id) {
        blockMap.put(location, (short)id);
        blockArray[index(location)] = (short)id;
    }

    void encodeMap(OutputStream chunkData) throws IOException {
        final var bitsPerBlock = 15;
        final int blocksPerLong = Long.SIZE / bitsPerBlock;
        for (int y = 0; y < 16; y++) {
            if (y == 0) { // block count
                Protocol.writeShort(chunkData, 256);
            } else {
                Protocol.writeShort(chunkData, 0);
            }
            Protocol.writeByte(chunkData, bitsPerBlock);
            Protocol.writeVarInt(chunkData, 1024);
            long acc = 0;
            int index = 0;
            for (int by = 0; by < 16; by++) {
                for (int bz = 0; bz < 16; bz++) {
                    for (int bx = 0; bx < 16; bx++) {
                        var l = new Location(bx, y*16 + by, bz);
                        var block = (long)blockMap.getOrDefault(l, (short)0);
                        acc |= block << (index * bitsPerBlock);
                        index += 1;
                        if (index >= blocksPerLong) {
                            Protocol.writeLong(chunkData, acc);
                            acc = 0;
                            index = 0;
                        }
                    }
                }
            }
        }
    }

    void encodeArray(OutputStream chunkData) throws IOException {
        final var bitsPerBlock = 15;
        final int blocksPerLong = Long.SIZE / bitsPerBlock;
        for (int y = 0; y < 16; y++) {
            if (y == 0) { // block count
                Protocol.writeShort(chunkData, 256);
            } else {
                Protocol.writeShort(chunkData, 0);
            }
            Protocol.writeByte(chunkData, bitsPerBlock);
            Protocol.writeVarInt(chunkData, 1024);
            long acc = 0;
            int index = 0;
            for (int by = 0; by < 16; by++) {
                for (int bz = 0; bz < 16; bz++) {
                    for (int bx = 0; bx < 16; bx++) {
                        var block = (long)blockArray[y * 4096 + by * 256 + bz * 16 + bx];
                        acc |= block << (index * bitsPerBlock);
                        index += 1;
                        if (index >= blocksPerLong) {
                            Protocol.writeLong(chunkData, acc);
                            acc = 0;
                            index = 0;
                        }
                    }
                }
            }
        }
    }
}
