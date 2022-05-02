package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.Inventory;
import com.pygostylia.osprey.MinecraftInputStream;
import com.pygostylia.osprey.MinecraftOutputStream;

import java.io.IOException;

public record WindowItemsPacket(int windowID, Inventory inventory) implements ClientBoundPacket {
    public static int initializeInventoryWindowID = 0;

    @Override
    public void encode(MinecraftOutputStream os) throws IOException {
        os.write(windowID);
        os.writeShort(inventory.size());
        for (int i = 0; i < inventory.size(); i++)
            inventory.get(i).encode(os);
    }
}
