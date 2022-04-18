package com.pygostylia.osprey.packets.v754

import com.pygostylia.osprey.ObjectEntity
import com.pygostylia.osprey.packets.ClientBoundPacket
import com.pygostylia.osprey.streams.MinecraftOutputStream
import java.io.OutputStream

class ClientBoundSpawnEntityPacket(
    val entity: ObjectEntity,
) : ClientBoundPacket() {
    override fun encode(o: OutputStream) {
        val output = MinecraftOutputStream(o)
        output.writeVarInt(entity.id())
        output.writeUUID(entity.uuid())
        output.writeVarInt(entity.type())
        output.writePosition(entity.position())

        // output.writeInt(entity.spawnData())
        output.writeInt(entity.spawnData())

        // output.writeVelocity(entity.velovity())
        output.writeShort(0)
        output.writeShort(0)
        output.writeShort(0)
    }
}