package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.MinecraftOutputStream;
import com.pygostylia.osprey.entities.ObjectEntity;

import java.io.IOException;

public record SpawnEntityPacket(ObjectEntity e) implements ClientBoundPacket {
    @Override
    public void encode(MinecraftOutputStream os) throws IOException {
        os.writeVarInt(e.id());
        os.writeUUID(e.uuid());
        os.writeVarInt(e.type());
        os.writePosition(e.entityPosition);
        os.writeVarInt(e.spawnData());
        os.writeVelocity(e.velocity);
    }
}
