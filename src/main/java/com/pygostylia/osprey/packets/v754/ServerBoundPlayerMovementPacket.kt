package com.pygostylia.osprey.packets.v754

import com.pygostylia.osprey.packets.ServerBoundPacket
import java.io.InputStream

class ServerBoundPlayerMovementPacket(i: InputStream) : ServerBoundPacket(i) {
    val onGround = input.readBoolean()
}