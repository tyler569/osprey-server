package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.MinecraftOutputStream;
import com.pygostylia.osprey.entities.Entity;
import org.json.JSONObject;

import java.io.IOException;

public record ChatMessagePacket(JSONObject chatData, Position position, Entity sender) implements ClientBoundPacket {
    public enum Position {
        ChatBox(0),
        SystemMessage(1),
        GameInfo(2);

        final byte position;

        Position(int p) {
            position = (byte) p;
        }
    }

    @Override
    public void encode(MinecraftOutputStream os) throws IOException {
        os.writeString(chatData.toString());
        os.write(position.position);
        os.writeUUID(sender.uuid());
    }
}
