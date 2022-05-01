package com.pygostylia.osprey;

import com.pygostylia.osprey.nbt.NBTCompound;

import java.io.IOException;

public record Slot(boolean empty, int itemId, int count, NBTCompound data) {
    public Slot() {
        this(true, 0, 0, null);
    }

    public Slot(int itemId, int count) {
        this(false, itemId, count, null);
    }

    public void encode(MinecraftOutputStream p) throws IOException {
        if (empty) {
            p.writeBoolean(false);
        } else {
            p.writeBoolean(true);
            p.writeVarInt(itemId);
            if (count > 255) {
                p.write(1);
            } else {
                p.write(count);
            }
            p.write(0); // NBTEnd, no NBT
        }
    }

    public static Slot from(MinecraftInputStream packet) throws IOException {
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

    public Slot decrement() {
        if (count == 1) {
            return new Slot();
        } else {
            return new Slot(itemId, count - 1);
        }
    }

    public Slot one() {
        if (count != 0) {
            return new Slot(itemId, 1);
        } else {
            return new Slot();
        }
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
