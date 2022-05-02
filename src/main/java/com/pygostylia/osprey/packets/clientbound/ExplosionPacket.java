package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.BlockPosition;
import com.pygostylia.osprey.MinecraftOutputStream;
import com.pygostylia.osprey.Velocity;

import java.io.IOException;
import java.util.Collection;

public record ExplosionPacket(BlockPosition position, float strength, Collection<BlockPosition> blocksDestroyed, Velocity playerKick) implements ClientBoundPacket {
    @Override
    public void encode(MinecraftOutputStream os) throws IOException {
        os.writeFloat((float) position.x());
        os.writeFloat((float) position.y());
        os.writeFloat((float) position.z());
        os.writeFloat(strength);
        os.writeInt(blocksDestroyed.size());
        for (var block : blocksDestroyed) {
            os.writeByte((byte) (block.x() - position.x()));
            os.writeByte((byte) (block.y() - position.y()));
            os.writeByte((byte) (block.z() - position.z()));
        }
        os.writeFloat(playerKick.x());
        os.writeFloat(playerKick.y());
        os.writeFloat(playerKick.z());
    }
}
