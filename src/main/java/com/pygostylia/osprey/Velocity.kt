package com.pygostylia.osprey

import com.pygostylia.osprey.streams.MinecraftOutputStream
import java.io.IOException
import kotlin.math.*

/*
The protocol representation of velocity is as a triple of Shorts,
representing velocity along the three axes in units of
1/8000 block / 50ms

Internally, I represent velocity as a float blocks / second, and
convert on-demand when encoding to the protocol layer.
 */
data class Velocity(val x: Float, val y: Float, val z: Float) {
    @Throws(IOException::class)
    fun write(os: MinecraftOutputStream) {
        os.writeShort(blockPerSecondToProtocol(x))
        os.writeShort(blockPerSecondToProtocol(y))
        os.writeShort(blockPerSecondToProtocol(z))
    }

    fun divide(dividend: Float): Velocity {
        return Velocity(x / dividend, y / dividend, z / dividend)
    }

    fun add(x: Float, y: Float, z: Float): Velocity {
        return Velocity(x + x, y + y, z + z)
    }

    fun offsetGravity(y: Float): Velocity {
        return add(0f, y, 0f)
    }

    companion object {
        @JvmStatic
        fun zero(): Velocity {
            return Velocity(0f, 0f, 0f)
        }

        @JvmStatic
        fun directionMagnitude(entityPosition: EntityPosition, speed: Float): Velocity {
            var x = -sin(entityPosition.yaw)
            var z = cos(entityPosition.yaw)
            val yH = -sin(entityPosition.pitch)
            val yXZ = cos(entityPosition.pitch)
            x *= yXZ
            z *= yXZ
            return Velocity(
                x.toFloat() * speed,
                yH.toFloat() * speed,
                z.toFloat() * speed
            )
        }

        private fun protocolToBlockPerSecond(protocol: Short): Float {
            return protocol / 400f
        }

        private fun blockPerSecondToProtocol(blockPerSecond: Float): Int {
            return (blockPerSecond * 400f).toInt()
        }
    }
}