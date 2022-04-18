package com.pygostylia.osprey.packets

import com.pygostylia.osprey.packets.v754.*
import java.lang.reflect.Constructor

enum class ClientBoundPacketID {
    SpawnEntity
}

class ProtocolVersion(
    val version: Int,
    val serverBoundpackets: Map<Int, Class<out ServerBoundPacket>>,
    val clientBoundPackets: Map<ClientBoundPacketID, Pair<Int, Class<out ClientBoundPacket>>>,
) {
    companion object {
        val V754 = ProtocolVersion(
            754,
            mapOf(
                0x14 to ServerBoundPlayerMovementPacket::class.java,
                0x15 to ServerBoundVehicleMovementPacket::class.java,
            ),
            mapOf(
                ClientBoundPacketID.SpawnEntity to Pair(0, ClientBoundSpawnEntityPacket::class.java),
            ),
        )
    }

    fun make(name: ClientBoundPacketID, vararg args: Any): ClientBoundPacket? {
        // val types = args.map { it.javaClass }.toTypedArray()
        val pair = clientBoundPackets[name] ?: return null
        // val packet = pair.second.getConstructor(*types)?.newInstance(*args)
        val packet = pair.second.constructors[0].newInstance(*args) as ClientBoundPacket
        packet.type = pair.first
        return packet
    }
}