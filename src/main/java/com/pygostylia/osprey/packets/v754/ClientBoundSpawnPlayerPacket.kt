package com.pygostylia.osprey.packets.v754

import com.pygostylia.osprey.ObjectEntity
import com.pygostylia.osprey.packets.ClientBoundPacket
import com.pygostylia.osprey.streams.MinecraftOutputStream
import java.io.OutputStream

class ClientBoundSpawnPlayerPacket(
    val entity: ObjectEntity,
) : ClientBoundPacket() {
    override fun encode(o: OutputStream) {
        val output = MinecraftOutputStream(o)

        output.writeVarInt(entity.id())
        output.writeUUID(entity.uuid())
        output.writePosition(entity.position())
    }
}