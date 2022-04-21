package com.pygostylia.osprey

import java.time.Duration

object EntityPosition {
  def middle(location: BlockPosition): EntityPosition = {
    val p = new EntityPosition
    p.x = location.x + 0.5
    p.y = location.y
    p.z = location.z + 0.5
    p
  }

  def orientation(yaw: Float, pitch: Float): EntityPosition = {
    val position = new EntityPosition
    position.yaw = yaw
    position.pitch = pitch
    position
  }
}

final case class EntityPosition(var onGround: Boolean, var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0, var pitch: Double = 0.0, var yaw: Double = 0.0) {
  def this(l: BlockPosition) {
    this(false, l.x, l.y, l.z)
  }

  def this(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
    this(false, x, y, z, pitch, yaw)
  }

  def this() = this(true)

  def moveTo(location: BlockPosition): Unit = {
    x = location.x + 0.5
    y = location.y
    z = location.z + 0.5
  }

  def yawAngle: Byte = (yaw / 360 * 256).toByte

  def pitchAngle: Byte = (pitch / 360 * 256).toByte

  def location = new BlockPosition(Math.floor(x).toInt, Math.floor(y).toInt, Math.floor(z).toInt)

  def chunkLocation = new ChunkLocation(chunkX, chunkZ)

  def chunkX: Int = x.toInt >> 4

  def chunkZ: Int = z.toInt >> 4

  def pitchRadians: Float = Math.toRadians(pitch).toFloat

  // override def toString: String = String.format("Position[x=%f, y=%f, z=%f, pitch=%f, yaw=%f]", x, y, z, pitch, yaw)

  def yawRadians: Float = Math.toRadians(yaw).toFloat

  def offset(dx: Double, dy: Double, dz: Double): EntityPosition = {
    val p = new EntityPosition(this)
    p.moveBy(dx, dy, dz)
    p
  }

  def this(p: EntityPosition) {
    this(p.onGround, p.x, p.y, p.z, p.pitch, p.yaw)
  }

  def moveBy(dx: Double, dy: Double, dz: Double): Unit = {
    x += dx
    y += dy
    z += dz
  }

  def updateFacing(dx: Double, dy: Double, dz: Double): Unit = {
    yaw = Math.toDegrees(Math.atan2(dx, dz)).toFloat
    pitch = Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))).toFloat
  }

  def stepVelocity(velocity: Velocity, timeStep: Duration): Unit = {
    val factor = timeStep.toNanos.toDouble / 1_000_000_000
    x += velocity.x / factor
    y += velocity.y / factor
    z += velocity.z / factor
  }
}
