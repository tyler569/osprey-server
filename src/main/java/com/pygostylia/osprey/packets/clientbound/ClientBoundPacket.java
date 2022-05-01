package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.MinecraftOutputStream;

import java.io.IOException;

public interface ClientBoundPacket {
    void encode(MinecraftOutputStream os) throws IOException;
}