package com.pygostylia.osprey

import java.util
import java.util.Random
import scala.jdk.CollectionConverters.IterableHasAsJava

object Explosion {
  private[osprey] val rng = new Random

  def generateBoomBlocks(center: BlockPosition, power: Float, maxRadius: Int = 20): util.Collection[BlockPosition] = {
    var points = for (x <- -maxRadius to maxRadius;
                      y <- -maxRadius to maxRadius;
                      z <- -maxRadius to maxRadius) yield center.offset(x, y, z)
    points = points.filter(p => 0 to 255 contains p.y)
    points = points.filter({ p =>
      val distance = center.distance(p)
      power / 1.5 > distance || rng.nextDouble() * power > distance
    })
    points
  }.asJavaCollection
}
