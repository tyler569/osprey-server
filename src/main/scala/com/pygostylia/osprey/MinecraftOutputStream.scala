package com.pygostylia.osprey

import java.io.{ByteArrayOutputStream, DataOutputStream, OutputStream}
import java.util.UUID

class MinecraftOutputStream(os: OutputStream) extends DataOutputStream(os) {
  def this() = this(new ByteArrayOutputStream())

  def writeVarInt(v: Int): Unit = {
    var i = v
    while (i > 0x7f || i < 0) {
      write(i & 0x7f | 0x80)
      i >>>= 7
    }
    write(i)
  }

  def writeString(s: String): Unit = {
    writeVarInt(s.length)
    writeBytes(s)
  }

  def writeBytes(bs: Array[Byte]): Unit = write(bs)

  def writeUUID(uuid: UUID): Unit = {
    writeLong(uuid.getMostSignificantBits)
    writeLong(uuid.getLeastSignificantBits)
  }

  def writeLocation(location: Location): Unit = {
    writeLong(location.encode)
  }

  def writeLocation(x: Int, y: Int, z: Int): Unit = {
    writeLocation(new Location(x, y, z))
  }

  def writePosition(position: Position): Unit = {
    writeDouble(position.x)
    writeDouble(position.y)
    writeDouble(position.z)
    writeByte(position.pitchAngle)
    writeByte(position.yawAngle)
  }

  def toByteArray: Array[Byte] = out.asInstanceOf[ByteArrayOutputStream].toByteArray
}
