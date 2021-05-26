package io.philbrick.minecraft;

import io.philbrick.minecraft.nbt.*;

import javax.sql.rowset.serial.*;
import java.io.*;
import java.nio.*;
import java.sql.*;
import java.util.*;
import java.util.zip.*;

public class Chunk {
    Map<Location, Short> blockMap;
    short[] heightMap;

    byte[] cachedPacket;
    boolean cacheValid;

    Chunk() {
        blockMap = new HashMap<>();
        heightMap = new short[256];
    }

    static Chunk defaultGeneration() {
        var c = new Chunk();
        for (int x = 0; x < 16; x += 1) {
            for (int z = 0; z < 16; z += 1) {
                for (int y = 0; y < 32; y++) {
                    c.setBlock(new Location(x, y, z), 1);
                }
            }
        }
        return c;
    }

    void setBlock(Location location, int id) {
        if (id == 0) {
            blockMap.remove(location);
        } else {
            blockMap.put(location, (short) id);
        }
        cacheValid = false;
    }

    void encodeMap(OutputStream chunkData) throws IOException {
        final var buffer = new ByteArrayOutputStream();
        final var bitsPerBlock = 15;
        final int blocksPerLong = Long.SIZE / bitsPerBlock;
        for (int y = 0; y < 16; y++) {
            long acc = 0;
            int index = 0;
            int count = 0;
            for (int by = 0; by < 16; by++) {
                for (int bz = 0; bz < 16; bz++) {
                    for (int bx = 0; bx < 16; bx++) {
                        var worldY = y*16 + by;
                        var l = new Location(bx, worldY, bz);
                        var block = (long)blockMap.getOrDefault(l, (short)0);
                        if (block != 0) {
                            count++;
                            heightMap[16*bz + bx] = (short)worldY;
                        }
                        acc |= block << (index * bitsPerBlock);
                        index += 1;
                        if (index >= blocksPerLong) {
                            Protocol.writeLong(buffer, acc);
                            acc = 0;
                            index = 0;
                        }
                    }
                }
            }
            Protocol.writeShort(chunkData, count); // blocks in chunk section
            Protocol.writeByte(chunkData, bitsPerBlock);
            Protocol.writeVarInt(chunkData, 1024);
            Protocol.writeBytes(chunkData, buffer.toByteArray());
            buffer.reset();
        }
    }

    NBTValue heightMapNBT() {
        var data = new Long[37];
        Arrays.fill(data, 0L);
        int index = 0;
        int dataIndex = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                data[dataIndex] |= ((long)heightMap[z*16 + x] << (index*9));
                index += 1;
                if (index == 7) {
                    index = 0;
                    dataIndex += 1;
                }
            }
        }
        return new NBTCompound(null,
            new NBTLongArray("MOTION_BLOCKING",
               data
            )
        );
    }

    byte[] encodeChunkData() throws IOException {
        var data = new ByteArrayOutputStream();
        encodeMap(data);
        return data.toByteArray();
    }

    void encodePacket(OutputStream m) throws IOException {
        if (cacheValid) {
            m.write(cachedPacket);
            return;
        }
        var buffer = new ByteArrayOutputStream();

        byte[] chunkData = encodeChunkData();
        NBTValue heightmap = heightMapNBT();

        Protocol.writeBoolean(buffer, true);
        Protocol.writeVarInt(buffer, 0xFFFF); // primary bitmask
        heightmap.encode(buffer);
        Protocol.writeVarInt(buffer, 1024);
        for (int i = 0; i < 1024; i++) {
            Protocol.writeVarInt(buffer, 0);
        }
        Protocol.writeVarInt(buffer, chunkData.length);
        Protocol.writeBytes(buffer, chunkData);
        Protocol.writeVarInt(buffer, 0);
        // Array of NBT containing block entities

        cachedPacket = buffer.toByteArray();
        cacheValid = true;
        m.write(cachedPacket);
    }

    byte[] encodeBlob() throws IOException {
        var inner = new ByteArrayOutputStream();
        var stream = new DeflaterOutputStream(inner);
        for (int y = 0; y < 256; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    Protocol.writeShort(stream, blockMap.getOrDefault(new Location(x, y, z), (short)0));
                }
            }
        }
        stream.finish();
        return inner.toByteArray();
    }

    static Chunk fromBlob(byte[] data) throws IOException {
        try {
            var stream = new InflaterInputStream(new ByteArrayInputStream(data));
            var c = new Chunk();
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        c.setBlock(new Location(x, y, z), Protocol.readShort(stream));
                    }
                }
            }
            return c;
        } catch (EOFException e) {
            System.out.println("Problem decoding chunk from disk!");
        }
        return null;
    }
}
