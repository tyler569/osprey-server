package io.philbrick.minecraft;

import io.philbrick.minecraft.nbt.*;

import java.io.*;

public class Slot {
    boolean empty;
    int itemId;
    int count;
    NBTValue data;

    Slot() {
        empty = true;
        data = new NBTEnd();
    }

    Slot(int item) {
        empty = false;
        itemId = item;
        count = 1;
        data = new NBTEnd();
    }

    // todo
    Slot(String itemName) {
        empty = false;
        itemId = 1;
        count = 1;
        data = new NBTEnd();
    }

    void setItem(int item) {
        itemId = item;
        count = 1;
    }

    void encode(OutputStream os) throws IOException {
        if (empty) {
            Protocol.writeBoolean(os, false);
        } else {
            Protocol.writeBoolean(os, true);
            Protocol.writeVarInt(os, itemId);
            Protocol.writeByte(os, count);
            data.encode(os);
        }
    }

    static Slot from(Packet packet) throws IOException {
        var p = new Slot();
        p.empty = !packet.readBoolean();
        if (!p.empty) {
            p.itemId = packet.readVarInt();
            p.count = packet.readShort();
            // TODO: p.data = packet.readNBT();
        }
        return p;
    }

    @Override
    public String toString() {
        return "Slot{" +
            "empty=" + empty +
            ", itemId=" + itemId +
            ", count=" + count +
            '}';
    }
}