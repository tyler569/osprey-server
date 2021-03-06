package com.pygostylia.osprey;

import com.pygostylia.osprey.nbt.NBTCompound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Chunk {
    static final int chunkSectionBlockCount = 16 * 16 * 16;
    static final int chunkBlockCount = chunkSectionBlockCount * 16;
    ByteBuffer blockData = ByteBuffer.allocate(chunkBlockCount * 2);
    ShortBuffer blockArray = blockData.asShortBuffer();
    short[] heightMap = new short[256];

    byte[] cachedPacket;
    boolean cacheValid;
    boolean modified;

    Chunk() {
    }

    static Integer diamond = Registry.blockDefaultId("minecraft:diamond_ore");

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
        blockArray.put(b, id);
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

    void setBlock(BlockPosition blockPosition, short id) {
        setBlock(blockPosition.blockIndex(), id);
    }

    void setBlock(BlockPosition blockPosition, int id) {
        setBlock(blockPosition, (short) id);
    }

    short block(BlockPosition blockPosition) {
        return blockArray.get(blockPosition.blockIndex());
    }

    void encodeMap(MinecraftOutputStream chunkData) throws IOException {
        final var buffer = new MinecraftOutputStream();
        final var bitsPerBlock = 15;
        final int blocksPerLong = Long.SIZE / bitsPerBlock;
        for (int chunkSectionY = 0; chunkSectionY < 16; chunkSectionY++) {
            long acc = 0;
            int index = 0;
            int count = 0;
            for (int by = 0; by < chunkSectionBlockCount; by++) {
                int blockIndex = chunkSectionY * chunkSectionBlockCount + by;
                var block = (long) blockArray.get(blockIndex);
                if (block != 0) {
                    count++;
                }
                acc |= block << (index * bitsPerBlock);
                index += 1;
                if (index >= blocksPerLong) {
                    buffer.writeLong(acc);
                    acc = 0;
                    index = 0;
                }
            }
            chunkData.writeShort(count); // blocks in chunk section
            chunkData.writeByte(bitsPerBlock);
            chunkData.writeVarInt(1024);
            chunkData.write(buffer.toByteArray());
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
        var data = new MinecraftOutputStream();
        encodeMap(data);
        return data.toByteArray();
    }

    public void encodePacket(OutputStream m) throws IOException {
        if (cacheValid) {
            m.write(cachedPacket);
            return;
        }
        var buffer = new MinecraftOutputStream();

        byte[] chunkData = encodeChunkData();
        var heightmap = heightMapNBT();

        buffer.writeBoolean(true);
        buffer.writeVarInt(0xFFFF); // primary bitmask
        heightmap.write(buffer);
        buffer.writeVarInt(1024);
        for (int i = 0; i < 1024; i++) {
            buffer.writeVarInt(0);
        }
        buffer.writeVarInt(chunkData.length);
        buffer.write(chunkData);
        buffer.writeVarInt(0);
        // Array of NBT containing block entities

        cachedPacket = buffer.toByteArray();
        cacheValid = true;
        m.write(cachedPacket);
    }

    byte[] encodeBlob() throws IOException {
        var inner = new ByteArrayOutputStream();
        var deflater = new DeflaterOutputStream(inner);
        deflater.write(blockData.array());
        deflater.finish();
        return inner.toByteArray();
    }

    static Chunk fromBlob(byte[] data) {
        try {
            var stream = new InflaterInputStream(new ByteArrayInputStream(data));
            var inflated = stream.readAllBytes();
            var c = new Chunk();

            c.blockData = ByteBuffer.wrap(inflated);
            c.blockArray = c.blockData.asShortBuffer();
            return c;
        } catch (Exception e) {
            System.out.println("Problem decoding chunk from disk!");
        }
        return null;
    }
}
