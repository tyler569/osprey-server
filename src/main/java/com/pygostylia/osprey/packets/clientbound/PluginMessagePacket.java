package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.MinecraftOutputStream;

import java.io.IOException;

public record PluginMessagePacket(String channel, byte[] data) implements ClientBoundPacket {
    @Override
    public void encode(MinecraftOutputStream os) throws IOException {
        os.writeString(channel);
        os.write(data);
    }
}
