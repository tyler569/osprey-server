package com.pygostylia.osprey

import java.io.{ByteArrayInputStream, InputStream}

class Packet(is: InputStream, val originalLen: Int) extends MinecraftInputStream(is) {
  val `type`: Int = readVarInt()

  def this(bs: Array[Byte]) = this(new ByteArrayInputStream(bs), bs.length)

  def this(bs: Array[Byte], originalLen: Int) = this(new ByteArrayInputStream(bs), originalLen)

  def packetType: Int = `type`
}