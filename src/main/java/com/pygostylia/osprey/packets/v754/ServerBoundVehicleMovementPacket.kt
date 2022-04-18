package com.pygostylia.osprey.packets.v754

import com.pygostylia.osprey.packets.ServerBoundPacket
import java.io.InputStream

class ServerBoundVehicleMovementPacket(i: InputStream) : ServerBoundPacket(i) {
    val x = input.readDouble()
    val y = input.readDouble()
    val z = input.readDouble()
    // each in degrees
    val yaw = input.readFloat()
    val pitch = input.readFloat()
}