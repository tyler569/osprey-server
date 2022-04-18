package com.pygostylia.osprey.packets

import java.io.OutputStream

abstract class ClientBoundPacket : Packet() {
    var type: Int? = null
        internal set

    abstract fun encode(o: OutputStream)
}