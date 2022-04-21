package com.pygostylia.osprey

final case class ChunkPosition(x: Int, z: Int) {
  def distanceFrom(location: ChunkPosition): Double = {
    math.hypot(math.abs(location.x - x), math.abs(location.z - z))
  }
}
