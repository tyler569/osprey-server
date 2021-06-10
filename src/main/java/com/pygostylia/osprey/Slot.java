package com.pygostylia.osprey;

import com.pygostylia.osprey.nbt.NBTCompound;

import java.io.IOException;
import java.io.OutputStream;

public record Slot(boolean empty, int itemId, int count, NBTCompound data) {
    public Slot() {
        this(true, 0, 0, null);
    }

    public Slot(int itemId, int count) {
        this(false, itemId, count, null);
    }

    void encode(OutputStream os) throws IOException {
        if (empty) {
            Protocol.writeBoolean(os, false);
        } else {
            Protocol.writeBoolean(os, true);
            Protocol.writeVarInt(os, itemId);
            if (count > 255) {
                Protocol.writeByte(os, 1);
            } else {
                Protocol.writeByte(os, count);
            }
            os.write(0); // NBTEnd, no NBT
        }
    }

    static Slot from(Packet packet) throws IOException {
        boolean hasEntry = packet.readBoolean();
        int itemId = 0;
        int count = 0;
        NBTCompound data = null;
        if (hasEntry) {
            itemId = packet.readVarInt();
            count = packet.read();
            int nbtEnd = packet.read();
            if (nbtEnd != 0) {
                System.out.println("You got an item with NBT! Better implement deserialization!");
            }
            // TODO: data = packet.readNBT();
        }
        return new Slot(!hasEntry, itemId, count, data);
    }

    @Override
    public String toString() {
        if (empty) {
            return "Slot[]";
        } else {
            return String.format("Slot[item=%s, count=%d]",
                    Registry.itemName(itemId), count);
        }
    }
}
