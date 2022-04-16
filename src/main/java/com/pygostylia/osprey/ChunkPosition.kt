package com.pygostylia.osprey

import kotlin.math.*

data class ChunkPosition(val x: Int, val z: Int) {
    fun distanceFrom(c: ChunkPosition): Double {
        return sqrt(
            abs(c.x - x).toDouble().pow(2.0) + abs(c.z - z).toDouble().pow(2.0)
        )
    }
}