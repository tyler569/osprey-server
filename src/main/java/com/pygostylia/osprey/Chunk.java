package com.pygostylia.osprey;

import com.pygostylia.osprey.nbt.NBTCompound;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Chunk {
    static final int chunkSectionBlockCount = 16 * 16 * 16;
    static final int chunkBlockCount = chunkSectionBlockCount * 16;
    short[] blockArray;
    short[] heightMap;

    byte[] cachedPacket;
    boolean cacheValid;
    boolean modified;

    Chunk() {
        blockArray = new short[chunkBlockCount];
        heightMap = new short[256];
    }

    static int diamond = Main.registry.blockDefaultId("minecraft:diamond_ore");

    static Chunk defaultGeneration() {
        var random = new Random();
        var c = new Chunk();
        for (int y = 0; y < 32; y++) {
            for (int z = 0; z < 16; z += 1) {
                for (int x = 0; x < 16; x += 1) {
                    if (random.nextFloat() < 0.0001) {
                        c.setBlock(x, y, z, diamond);
                    } else {
                        c.setBlock(x, y, z, 1);
                    }
                }
            }
        }
        return c;
    }

    void setBlock(int b, short id) {
        blockArray[b] = id;
        cacheValid = false;
        modified = true;
    }

    void setBlock(int b, int id) {
        setBlock(b, (short) id);
    }

    void setBlock(int x, int y, int z, short id) {
        setBlock(y * 256 + z * 16 + x, id);
    }

    void setBlock(int x, int y, int z, int id) {
        setBlock(x, y, z, (short) id);
    }

    void setBlock(Location location, short id) {
        setBlock(location.blockIndex(), id);
    }

    void setBlock(Location location, int id) {
        setBlock(location, (short) id);
    }

    short block(Location location) {
        return blockArray[location.blockIndex()];
    }

    void encodeMap(OutputStream chunkData) throws IOException {
        final var buffer = new ByteArrayOutputStream();
        final var bitsPerBlock = 15;
        final int blocksPerLong = Long.SIZE / bitsPerBlock;
        for (int chunkSectionY = 0; chunkSectionY < 16; chunkSectionY++) {
            long acc = 0;
            int index = 0;
            int count = 0;
            for (int by = 0; by < chunkSectionBlockCount; by++) {
                int blockIndex = chunkSectionY * chunkSectionBlockCount + by;
                var block = (long) blockArray[blockIndex];
                if (block != 0) {
                    count++;
                }
                acc |= block << (index * bitsPerBlock);
                index += 1;
                if (index >= blocksPerLong) {
                    Protocol.writeLong(buffer, acc);
                    acc = 0;
                    index = 0;
                }
            }
            Protocol.writeShort(chunkData, count); // blocks in chunk section
            Protocol.writeByte(chunkData, bitsPerBlock);
            Protocol.writeVarInt(chunkData, 1024);
            Protocol.writeBytes(chunkData, buffer.toByteArray());
            buffer.reset();
        }
    }

    NBTCompound heightMapNBT() {
        var data = new Long[37];
        Arrays.fill(data, 0L);
        int index = 0;
        int dataIndex = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                data[dataIndex] |= ((long) heightMap[z * 16 + x] << (index * 9));
                index += 1;
                if (index == 7) {
                    index = 0;
                    dataIndex += 1;
                }
            }
        }
        var compound = new NBTCompound();
        compound.put("MOTION_BLOCKING", data);
        return compound;
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
        var heightmap = heightMapNBT();

        Protocol.writeBoolean(buffer, true);
        Protocol.writeVarInt(buffer, 0xFFFF); // primary bitmask
        heightmap.write(buffer);
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
        var deflater = new DeflaterOutputStream(inner);
        var buffer = ByteBuffer.allocate(chunkBlockCount * Short.BYTES);
        buffer.asShortBuffer().put(blockArray);
        deflater.write(buffer.array());
        deflater.finish();
        return inner.toByteArray();
    }

    static Chunk fromBlob(byte[] data) throws IOException {
        try {
            var stream = new InflaterInputStream(new ByteArrayInputStream(data));
            var inflated = stream.readAllBytes();
            var c = new Chunk();

            var buffer = ByteBuffer.wrap(inflated);
            buffer.asShortBuffer().get(c.blockArray);
            return c;
        } catch (EOFException e) {
            System.out.println("Problem decoding chunk from disk!");
        }
        return null;
    }
}
