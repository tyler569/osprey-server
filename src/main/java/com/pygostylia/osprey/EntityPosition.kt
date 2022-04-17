package com.pygostylia.osprey

import java.time.Duration
import kotlin.math.*

data class EntityPosition(
    var x: Double = 0.5,
    var y: Double = 32.0,
    var z: Double = 0.5,
    var pitch: Double = 0.0,
    var yaw: Double = 0.0,
    var onGround: Boolean = false,
) {
    constructor(blockPosition: BlockPosition) : this(
        blockPosition.x.toDouble(), blockPosition.y.toDouble(), blockPosition.z.toDouble()
    )

    val pitchDegrees: Double get() = Math.toDegrees(pitch)
    val yawDegrees: Double get() = Math.toDegrees(yaw)
    fun pitchAngle(): Byte = (pitchDegrees / 360 * 256).toInt().toByte()
    fun yawAngle(): Byte = (yawDegrees / 360 * 256).toInt().toByte()

    fun moveTo(blockPosition: BlockPosition) {
        x = blockPosition.x + 0.5
        y = blockPosition.y.toDouble()
        z = blockPosition.z + 0.5
    }

    val chunkX: Int get() = x.toInt() shr 4
    val chunkZ: Int get() = z.toInt() shr 4
    val chunkPosition: ChunkPosition get() = ChunkPosition(chunkX, chunkZ)
    val blockPosition: BlockPosition get() = BlockPosition(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())

    @Deprecated("Use the pitch parameter", ReplaceWith("pitch"))
    fun pitchRadians(): Double = pitch
    @Deprecated("Use the yaw parameter", ReplaceWith("yaw"))
    fun yawRadians(): Double = yaw
    @Deprecated("Move to parameter chunkX", ReplaceWith("chunkX"))
    fun chunkX(): Int = chunkX
    @Deprecated("Move to parameter chunkZ", ReplaceWith("chunkZ"))
    fun chunkZ(): Int = chunkZ
    @Deprecated("Move to parameter chunkPosition", ReplaceWith("chunkPosition"))
    fun chunkPosition() = chunkPosition
    @Deprecated("Move to parameter blockPosition", ReplaceWith("blockPosition"))
    fun blockPosition() = blockPosition

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
        yaw = Math.toDegrees(atan2(dx, dz))
        pitch = Math.toDegrees(atan2(dy, hypot(dx, dz)))
    }

    fun stepVelocity(velocity: Velocity, timeStep: Duration) {
        val factor = timeStep.toNanos().toDouble() / 1_000_000_000
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
        fun orientation(yaw: Double, pitch: Double): EntityPosition {
            val entityPosition = EntityPosition()
            entityPosition.yaw = yaw
            entityPosition.pitch = pitch
            return entityPosition
        }
    }
}