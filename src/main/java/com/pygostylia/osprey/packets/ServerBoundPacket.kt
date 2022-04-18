package com.pygostylia.osprey.packets

import com.pygostylia.osprey.streams.MinecraftInputStream
import java.io.InputStream

open class ServerBoundPacket(i: InputStream) : Packet() {
    val input = MinecraftInputStream(i)
}