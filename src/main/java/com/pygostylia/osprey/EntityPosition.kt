package com.pygostylia.osprey

import java.time.Duration

data class EntityPosition(
    var x: Double = 0.5,
    var y: Double = 32.0,
    var z: Double = 0.5,
    var pitch: Float = 0f,
    var yaw: Float = 0f,
    var onGround: Boolean = false,
) {
    constructor(blockPosition: BlockPosition) : this(blockPosition.x.toDouble(), blockPosition.y.toDouble(), blockPosition.z.toDouble())

    fun moveTo(blockPosition: BlockPosition) {
        x = blockPosition.x + 0.5
        y = blockPosition.y.toDouble()
        z = blockPosition.z + 0.5
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

    fun blockPosition(): BlockPosition {
        return BlockPosition(Math.floor(x).toInt(), Math.floor(y).toInt(), Math.floor(z).toInt())
    }

    fun chunkPosition(): ChunkPosition {
        return ChunkPosition(chunkX(), chunkZ())
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

    fun offset(dx: Double, dy: Double, dz: Double): EntityPosition {
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
        fun middle(blockPosition: BlockPosition): EntityPosition {
            val p = EntityPosition()
            p.x = blockPosition.x + 0.5
            p.y = blockPosition.y.toDouble()
            p.z = blockPosition.z + 0.5
            return p
        }

        @JvmStatic
        fun orientation(yaw: Float, pitch: Float): EntityPosition {
            val entityPosition = EntityPosition()
            entityPosition.yaw = yaw
            entityPosition.pitch = pitch
            return entityPosition
        }
    }
}