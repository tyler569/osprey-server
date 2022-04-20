package com.pygostylia.osprey

final class ChunkLocation(val x: Int, val z: Int) {
  def distanceFrom(location: ChunkLocation): Double = {
    Math.sqrt(Math.pow(Math.abs(location.x - x), 2) + Math.pow(Math.abs(location.z - z), 2))
  }
}
