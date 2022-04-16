package com.pygostylia.osprey

data class Location(val x: Int, val y: Int, val z: Int) {
    internal constructor(protocolLocation: Long) : this(
        (protocolLocation shr 38).toInt(),
        (protocolLocation and 0xFFFL).toInt(),
        (protocolLocation shl 26 shr 38).toInt()
    )

    fun chunkX() = x shr 4
    fun chunkZ() = z shr 4

    fun chunkLocation(): ChunkLocation {
        return ChunkLocation(chunkX(), chunkZ())
    }

    fun offsetByChunks(dx: Int, dz: Int): Location {
        return Location(x + dx * 16, y, z + dz * 16)
    }

    fun offset(dx: Int, dy: Int, dz: Int): Location {
        return Location(x + dx, y + dy, z + dz)
    }

    fun offset(direction: Direction, delta: Int) = when (direction) {
        Direction.North -> Location(x, y, z - delta)
        Direction.South -> Location(x, y, z + delta)
        Direction.West -> Location(x - delta, y, z)
        Direction.East -> Location(x + delta, y, z)
        Direction.Up -> Location(x, y + delta, z)
        Direction.Down -> Location(x, y - delta, z)
    }

    fun positionInChunk(): Location {
        return Location(x and 0xF, y, z and 0xF)
    }

    fun encode(): Long {
        return (x and 0x3FFFFFF).toLong() shl 38 or ((z and 0x3FFFFFF).toLong() shl 12) or (y and 0xFFF).toLong()
    }

    fun blockIndex(): Int {
        return y * 256 + z * 16 + x
    }

    fun withinRadiusOf(radius: Int, location: Location): Boolean {
        return Math.abs(location.x - x) < radius && Math.abs(location.z - z) < radius
    }

    fun distance(other: Location): Double {
        return Math.sqrt(
            Math.pow((x - other.x).toDouble(), 2.0) +
                    Math.pow((y - other.y).toDouble(), 2.0) +
                    Math.pow((z - other.z).toDouble(), 2.0)
        )
    }

    companion object {
        private fun parseRelative(current: Int, str: String): Int {
            return if (str.startsWith("~")) {
                if (str.length == 1) {
                    current
                } else {
                    (current + str.substring(1).toFloat()).toInt()
                }
            } else {
                str.toFloat().toInt()
            }
        }

        @JvmStatic
        fun relativeLocation(base: Location, args: Array<String>): Location {
            val x: Int
            val y: Int
            val z: Int
            x = parseRelative(base.x, args[0])
            y = parseRelative(base.y, args[1])
            z = parseRelative(base.z, args[2])
            return Location(x, y, z)
        }
    }
}