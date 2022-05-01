package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.BlockPosition;
import com.pygostylia.osprey.BlockState;
import com.pygostylia.osprey.MinecraftOutputStream;

import java.io.IOException;

public record AcknowlegePlayerDiggingPacket(BlockPosition pos, BlockState now, DiggingStatus status, boolean success)
        implements ClientBoundPacket {
    @Override
    public void encode(MinecraftOutputStream os) throws IOException {
        os.writePosition(pos);
        os.writeVarInt(now.protocolId());
        os.writeVarInt(status.protocolValue);
        os.writeBoolean(success);
    }

    public enum DiggingStatus {
        Started(0),
        Cancelled(1),
        Finished(2);

        public final int protocolValue;

        DiggingStatus(int v) {
            protocolValue = v;
        }
    }
}