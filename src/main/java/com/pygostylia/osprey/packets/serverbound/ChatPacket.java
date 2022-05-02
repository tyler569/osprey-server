package com.pygostylia.osprey.packets.serverbound;

import com.pygostylia.osprey.MinecraftInputStream;

import java.io.IOException;

public final class ChatPacket implements ServerBoundPacket {
    private final String message;

    public ChatPacket(MinecraftInputStream is) throws IOException {
        message = is.readString();
    }

    public String message() {
        return message;
    }
}
