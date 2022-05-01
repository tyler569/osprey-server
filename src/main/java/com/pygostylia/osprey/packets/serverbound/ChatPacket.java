package com.pygostylia.osprey.packets.serverbound;

import com.pygostylia.osprey.MinecraftInputStream;

import java.io.IOException;

public class ChatPacket extends ServerBoundPacket {
    String message;

    public ChatPacket(MinecraftInputStream is) throws IOException {
        message = is.readString();
    }
}
