package com.pygostylia.osprey

import java.io.{ByteArrayInputStream, DataInputStream, InputStream}

class MinecraftInputStream(is: InputStream) extends DataInputStream(is) {
  def this(bs: Array[Byte]) = this(new ByteArrayInputStream(bs))

  def readVarInt(): Int = {
    var i = 0
    var n = 0
    while (true) {
      val b = read()
      if (0 to 127 contains b) {
        i |= b << (7 * n)
        return i
      }
      i |= (b & 0x7f) << (7 * n)
      n += 1
    }
    i
  }

  def readString(): String = {
    val len = readVarInt()
    val bytes = readNBytes(len)
    new String(bytes)
  }

  def readLocation(): BlockPosition = {
    new BlockPosition(readLong())
  }

  def readPosition(): EntityPosition = {
    val x = readDouble()
    val y = readDouble()
    val z = readDouble()
    val yaw = readFloat()
    val pitch = readFloat()
    new EntityPosition(x, y, z, yaw, pitch)
  }
}
