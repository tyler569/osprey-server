package com.pygostylia.osprey

import com.pygostylia.osprey.commands.CommandBucket
import org.json.JSONArray
import org.json.JSONObject
import java.net.ServerSocket
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.stream.Stream

object Main {
    // TODO: read these in from configuration
    const val brand = "Osprey"
    private val players: MutableMap<Int, Player> = ConcurrentHashMap()
    private val entities: MutableMap<Int, Entity> = ConcurrentHashMap()

    val encryptionKey: KeyPair by lazy {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.generateKeyPair()
    }

    val world: World by lazy { World.open("world.db") }
    val commands: CommandBucket = CommandBucket()

    private var nextEntityId = 1
    var scheduler = Scheduler()
    fun addPlayer(player: Player) {
        players[player.id()] = player
    }

    fun removePlayer(player: Player) {
        players.remove(player.id())
    }

    fun addEntity(entity: Entity): Int {
        val id = nextEntityId++
        entities[id] = entity
        return id
    }

    fun removeEntity(entity: Entity) {
        entities.remove(entity.id())
    }

    fun allPlayers(): Collection<Player> = players.values
    fun playerByName(name: String): Player? = players.values.firstOrNull { it.name == name }
    fun playerByEntityId(entityId: Int): Player? = players[entityId]
    fun entityById(entityId: Int): Optional<Entity> = Optional.ofNullable(entities[entityId])
    fun forEachPlayer(lambda: Consumer<Player>) = players.values.forEach(lambda)

    fun playersWithin(radius: Int, blockPosition: BlockPosition): Stream<Player> =
        players.values.stream().filter { player -> player.location().withinRadiusOf(radius, blockPosition) }

    fun handshakeJson(): String {
        val result = JSONObject()
        val version = JSONObject()
        val players = JSONObject()
        val description = JSONObject()
        val playerSample = JSONArray()
        version.put("name", "1.16.5")
        version.put("protocol", 754)
        forEachPlayer { player ->
            val playerJson = JSONObject()
            playerJson.put("name", player.name())
            playerJson.put("id", player.uuid())
            playerSample.put(playerJson)
        }
        players.put("max", 10)
        players.put("online", Main.players.size)
        players.put("sample", playerSample)
        description.put("text", "Hello World!")
        result.put("version", version)
        result.put("players", players)
        result.put("description", description)
        return result.toString()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        Registry.setup("generated")

        BlockState.setup()

        commands.register(Commands.javaClass)

        Thread(scheduler).start()
        Thread(BackgroundJob).start()

        val socket = ServerSocket(25565)
        println("Ready - Listening on ${socket.localSocketAddress}")
        while (!socket.isClosed) {
            val connection = socket.accept()
            Player.runThread(connection)
        }
    }
}