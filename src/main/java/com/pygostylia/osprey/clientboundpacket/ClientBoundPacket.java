package com.pygostylia.osprey.clientboundpacket;

import com.pygostylia.osprey.MinecraftOutputStream;

import java.io.IOException;

public abstract class ClientBoundPacket {
    abstract public void encode(MinecraftOutputStream os) throws IOException;
}
