package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.MinecraftOutputStream;
import com.pygostylia.osprey.entities.Entity;

import java.io.IOException;

public record EntityStatusPacket(Entity entity, byte status) implements ClientBoundPacket {
    @Override
    public void encode(MinecraftOutputStream os) throws IOException {
        os.writeInt(entity.id());
        os.write(status);
    }
}
