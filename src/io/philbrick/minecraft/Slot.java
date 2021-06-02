package io.philbrick.minecraft;

import io.philbrick.minecraft.nbt.*;

import java.io.*;

public class Slot {
    boolean empty;
    int itemId;
    int count;
    NBTCompound data;

    Slot() {
        empty = true;
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
            if (count > 255) {
                Protocol.writeByte(os, 1);
            } else {
                Protocol.writeByte(os, count);
            }
            os.write(0); // NBTEnd, no NBT
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
