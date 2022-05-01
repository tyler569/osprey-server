package com.pygostylia.osprey.severboundpacket;

import com.pygostylia.osprey.MinecraftInputStream;
import com.pygostylia.osprey.MinecraftOutputStream;

import java.io.IOException;

public class ChatPacket extends ServerBoundPacket {
    String message;

    public ChatPacket(MinecraftInputStream is) throws IOException {
        message = is.readString();
    }
}
