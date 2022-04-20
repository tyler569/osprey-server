package com.pygostylia.osprey

import java.util

object BoatEntity {
  private[osprey] val TYPE = Registry.entity("minecraft:boat")
}

final class BoatEntity(p: Position) extends ObjectEntity(p) {
  private[osprey] val passengers = new util.ArrayList[Entity]
  var turningLeft = false
  var turningRight = false
  position = p

  override def spawnForPlayer(player: Player): Unit = {
    super.spawnForPlayer(player)
    if (passengers.isEmpty) updatePassengers(player)
  }

  override def interact(sender: Player): Unit = addPassenger(sender)

  def addPassenger(passenger: Entity): Unit = {
    passengers.add(passenger)
    passenger match {
      case player: Player => player.setRidingEntity(this)
    }
    updatePassengers()
  }

  private[osprey] def updatePassengers(): Unit =
    playersWithLoaded.forEach(this.updatePassengers)

  private[osprey] def updatePassengers(player: Player): Unit =
    player.sendSetPassengers(this, passengers)

  def dismount(sender: Player): Unit = removePassenger(sender)

  def removePassenger(passenger: Entity): Unit = {
    passengers.remove(passenger)
    passenger match {
      case player: Player => player.setNotRidingEntity()
    }
    updatePassengers()
  }

  override def attack(sender: Player): Unit = destroy()

  override private[osprey] def `type` = BoatEntity.TYPE

  override private[osprey] def colliderXZ = 1.375f

  override private[osprey] def colliderY = 0.5625f
}
