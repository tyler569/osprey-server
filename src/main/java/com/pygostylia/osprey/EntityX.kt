package com.pygostylia.osprey

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

abstract class EntityX(val registryName: String, val collider: Collider) : Entity() {
    companion object {
        var nextEntityId = 1
        val allEntities: ConcurrentHashMap<Int, EntityX> = ConcurrentHashMap()
    }

    val idx: Int = nextEntityId++

    init {
        allEntities[idx] = this
    }

    val typex: Int by lazy { Registry.entity(registryName) }
    val uuidx: UUID by lazy { UUID.randomUUID() }
    var entityPosition: EntityPosition = EntityPosition()
    val playersWithLoadedx: ArrayList<PlayerX> = ArrayList()

    // Temporary overrides of Entity methods during transition
    override fun colliderXZ(): Float = collider.xz
    override fun colliderY(): Float = collider.y
    override fun type(): Int = typex
    override fun uuid(): UUID = uuidx

    fun blockPosition() = entityPosition.blockPosition

    open fun spawnForPlayer(p: PlayerX) {
        playersWithLoadedx.add(p)
    }

    fun spawn() {
        BackgroundJob.queueHighPriority {
            Main.playersWithin(64, blockPosition()).forEach { player ->
                spawnForPlayer(player)
            }
        }
    }

    open fun doAIUpdate() {}
}

private class LightningX: EntityX("minecraft:lightning_bolt", Collider(y = 0F, xz = 0F))

private class PigX: EntityX("minecraft:pig", Collider(y = 1F, xz = 1F)) {
    override fun doAIUpdate() {
        // Do some super advanced pig AI
    }
}
