package com.pygostylia.osprey

object Location {
  def relativeLocation(base: Location, args: Array[String]): Location = {
    val x = parseRelative(base.x, args(0))
    val y = parseRelative(base.y, args(1))
    val z = parseRelative(base.z, args(2))
    new Location(x, y, z)
  }

  private def parseRelative(current: Int, str: String) = {
    if (str.startsWith("~")) {
      if (str.length == 1) current else str.substring(1).toFloat
    } else str.toFloat
  }.toInt
}

final case class Location(x: Int, y: Int, z: Int) {
  def this(protocolLocation: Long) {
    this((protocolLocation >> 38).toInt, (protocolLocation & 0xFFF).toInt, ((protocolLocation << 26) >> 38).toInt)
  }

  def chunkLocation = new ChunkLocation(chunkX, chunkZ)

  def chunkX: Int = x >> 4

  def chunkZ: Int = z >> 4

  def offsetByChunks(dx: Int, dz: Int) = new Location(x + dx * 16, y, z + dz * 16)

  def offset(dx: Int, dy: Int, dz: Int) = new Location(x + dx, y + dy, z + dz)

  def offset(direction: Direction, delta: Int): Location = direction match {
    case Direction.North => new Location(x, y, z - delta)
    case Direction.South => new Location(x, y, z + delta)
    case Direction.West => new Location(x - delta, y, z)
    case Direction.East => new Location(x + delta, y, z)
    case Direction.Up => new Location(x, y + delta, z)
    case Direction.Down => new Location(x, y - delta, z)
  }

  def positionInChunk = new Location(x & 0xF, y, z & 0xF)

  def encode: Long = ((x & 0x3FFFFFF).toLong << 38) | ((z & 0x3FFFFFF).toLong << 12) | (y & 0xFFF)

  def blockIndex: Int = y * 256 + z * 16 + x

  def withinRadiusOf(radius: Int, location: Location): Boolean = {
    Math.abs(location.x - x) < radius && Math.abs(location.z - z) < radius
  }

  def distance(other: Location): Double = {
    Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2))
  }
}
