package com.pygostylia.osprey.packets.clientbound;

import com.pygostylia.osprey.MinecraftOutputStream;
import org.json.JSONObject;

import java.io.IOException;

public record DisconnectPacket(JSONObject reason) implements ClientBoundPacket {
    @Override
    public void encode(MinecraftOutputStream os) throws IOException {
        os.writeString(reason.toString());
    }
}
