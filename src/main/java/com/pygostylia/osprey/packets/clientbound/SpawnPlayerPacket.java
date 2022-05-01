package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.MinecraftOutputStream;
import com.pygostylia.osprey.entities.Player;

import java.io.IOException;

public record SpawnPlayerPacket(Player p) implements ClientBoundPacket {
    @Override
    public void encode(MinecraftOutputStream os) throws IOException {
        os.writeVarInt(p.id());
        os.writeUUID(p.uuid());
        os.writePosition(p.entityPosition());
    }
}
