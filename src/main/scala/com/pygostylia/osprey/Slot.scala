package com.pygostylia.osprey

import com.pygostylia.osprey.nbt.NBTCompound


object Slot {
  def from(packet: Packet): Slot = {
    val hasEntry = packet.readBoolean
    var itemId = 0
    var count = 0
    val data = null
    if (hasEntry) {
      itemId = packet.readVarInt
      count = packet.read
      val nbtEnd = packet.read
      if (nbtEnd != 0) System.out.println("You got an item with NBT! Better implement deserialization!")
      // TODO: data = packet.readNBT();
    }
    new Slot(!hasEntry, itemId, count, data)
  }
}

final case class Slot(empty: Boolean, itemId: Int = 0, count: Int = 0, data: NBTCompound = null) {
  def this(itemId: Int, count: Int) {
    this(false, itemId, count, null)
  }

  def this() = this(true)

  def encode(p: PacketBuilder): Unit = {
    if (empty) p.writeBoolean(false)
    else {
      p.writeBoolean(true)
      p.writeVarInt(itemId)
      if (count > 255) p.write(1)
      else p.write(count)
      p.write(0) // NBTEnd, no NBT

    }
  }

  def decrement: Slot = if (count == 1) {
    new Slot(true)
  } else {
    new Slot(itemId, count - 1)
  }

  def one: Slot = if (count != 0) {
    new Slot(itemId, 1)
  } else {
    new Slot()
  }

  override def toString: String = {
    if (empty) "Slot[]"
    else f"Slot[item=${Registry.itemName(itemId)}, count=${count}"
  }
}

