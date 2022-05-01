package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.MinecraftOutputStream;
import com.pygostylia.osprey.entities.Entity;

import java.io.IOException;

public record EntityAnimationPacket(Entity e, int animation) implements ClientBoundPacket {
    @Override
    public void encode(MinecraftOutputStream os) throws IOException {
        os.writeVarInt(e.id());
        os.write((byte) animation);
    }
}
