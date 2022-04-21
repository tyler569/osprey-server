package com.pygostylia.osprey

object BlockPosition {
  def relativeLocation(base: BlockPosition, args: Array[String]): BlockPosition = {
    val x = parseRelative(base.x, args(0))
    val y = parseRelative(base.y, args(1))
    val z = parseRelative(base.z, args(2))
    new BlockPosition(x, y, z)
  }

  private def parseRelative(current: Int, str: String) = {
    if (str.startsWith("~")) {
      if (str.length == 1) current else str.substring(1).toFloat
    } else str.toFloat
  }.toInt
}

final case class BlockPosition(x: Int, y: Int, z: Int) {
  def this(protocolLocation: Long) {
    this((protocolLocation >> 38).toInt, (protocolLocation & 0xFFF).toInt, ((protocolLocation << 26) >> 38).toInt)
  }

  def chunkLocation = new ChunkPosition(chunkX, chunkZ)

  def chunkX: Int = x >> 4

  def chunkZ: Int = z >> 4

  def offsetByChunks(dx: Int, dz: Int) = new BlockPosition(x + dx * 16, y, z + dz * 16)

  def offset(dx: Int, dy: Int, dz: Int) = new BlockPosition(x + dx, y + dy, z + dz)

  def offset(direction: Direction, delta: Int): BlockPosition = direction match {
    case Direction.North => new BlockPosition(x, y, z - delta)
    case Direction.South => new BlockPosition(x, y, z + delta)
    case Direction.West => new BlockPosition(x - delta, y, z)
    case Direction.East => new BlockPosition(x + delta, y, z)
    case Direction.Up => new BlockPosition(x, y + delta, z)
    case Direction.Down => new BlockPosition(x, y - delta, z)
  }

  def positionInChunk = new BlockPosition(x & 0xF, y, z & 0xF)

  def encode: Long = ((x & 0x3FFFFFF).toLong << 38) | ((z & 0x3FFFFFF).toLong << 12) | (y & 0xFFF)

  def blockIndex: Int = y * 256 + z * 16 + x

  def withinRadiusOf(radius: Int, location: BlockPosition): Boolean = {
    Math.abs(location.x - x) < radius && Math.abs(location.z - z) < radius
  }

  def distance(other: BlockPosition): Double = {
    Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2))
  }
}
