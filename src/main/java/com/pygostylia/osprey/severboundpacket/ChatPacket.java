package com.pygostylia.osprey.severboundpacket;

public class ChatPacket extends ServerBoundPacket {
    String message;

    public ChatPacket(String msg) {
        message = msg;
    }
}
