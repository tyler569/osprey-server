package com.pygostylia.osprey

import java.io.IOException


/*
The protocol representation of velocity is as a triple of Shorts,
representing velocity along the three axes in units of
1/8000 block / 50ms

Internally, I represent velocity as a float blocks / second, and
convert on-demand when encoding to the protocol layer.
 */
object Velocity {
  def zero = new Velocity(0, 0, 0)

  def directionMagnitude(position: Position, speed: Float): Velocity = {
    var x = -Math.sin(position.yawRadians)
    var z = Math.cos(position.yawRadians)
    val yH = -Math.sin(position.pitchRadians)
    val yXZ = Math.cos(position.pitchRadians)
    x *= yXZ
    z *= yXZ
    new Velocity(x.toFloat * speed, yH.toFloat * speed, z.toFloat * speed)
  }

  private def protocolToBlockPerSecond(protocol: Short) = protocol / 400f

  private def blockPerSecondToProtocol(blockPerSecond: Float) = (blockPerSecond * 400f).toShort
}

final case class Velocity(x: Float, y: Float, z: Float) {
  def write(os: PacketBuilder): Unit = {
    os.writeShort(Velocity.blockPerSecondToProtocol(x))
    os.writeShort(Velocity.blockPerSecondToProtocol(y))
    os.writeShort(Velocity.blockPerSecondToProtocol(z))
  }

  def divide(dividend: Float) = new Velocity(x / dividend, y / dividend, z / dividend)

  def add(x: Float, y: Float, z: Float) = new Velocity(this.x + x, this.y + y, this.z + z)

  def offsetGravity(y: Float): Velocity = this.add(0, y, 0)
}
