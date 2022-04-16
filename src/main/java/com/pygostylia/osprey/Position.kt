package com.pygostylia.osprey

import java.time.Duration

data class Position(
    var x: Double = 0.5,
    var y: Double = 32.0,
    var z: Double = 0.5,
    var pitch: Float = 0f,
    var yaw: Float = 0f,
    var onGround: Boolean = false,
) {
    constructor(location: Location) : this(location.x.toDouble(), location.y.toDouble(), location.z.toDouble())

    fun moveTo(location: Location) {
        x = location.x + 0.5
        y = location.y.toDouble()
        z = location.z + 0.5
    }

    fun yawAngle(): Byte {
        return (yaw / 360 * 256).toInt().toByte()
    }

    fun pitchAngle(): Byte {
        return (pitch / 360 * 256).toInt().toByte()
    }

    fun chunkX(): Int {
        return x.toInt() shr 4
    }

    fun chunkZ(): Int {
        return z.toInt() shr 4
    }

    fun location(): Location {
        return Location(Math.floor(x).toInt(), Math.floor(y).toInt(), Math.floor(z).toInt())
    }

    fun chunkLocation(): ChunkLocation {
        return ChunkLocation(chunkX(), chunkZ())
    }

    fun pitchRadians(): Float {
        return Math.toRadians(pitch.toDouble()).toFloat()
    }

    fun yawRadians(): Float {
        return Math.toRadians(yaw.toDouble()).toFloat()
    }

    fun moveBy(dx: Double, dy: Double, dz: Double) {
        x += dx
        y += dy
        z += dz
    }

    fun offset(dx: Double, dy: Double, dz: Double): Position {
        val p = copy()
        p.moveBy(dx, dy, dz)
        return p
    }

    fun updateFacing(dx: Double, dy: Double, dz: Double) {
        yaw = Math.toDegrees(Math.atan2(dx, dz)).toFloat()
        pitch = Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))).toFloat()
    }

    fun stepVelocity(velocity: Velocity, timeStep: Duration) {
        val factor = timeStep.toNanos().toDouble() / 1000000000
        x += velocity.x / factor
        y += velocity.y / factor
        z += velocity.z / factor
    }

    companion object {
        @JvmStatic
        fun middle(location: Location): Position {
            val p = Position()
            p.x = location.x + 0.5
            p.y = location.y.toDouble()
            p.z = location.z + 0.5
            return p
        }

        @JvmStatic
        fun orientation(yaw: Float, pitch: Float): Position {
            val position = Position()
            position.yaw = yaw
            position.pitch = pitch
            return position
        }
    }
}