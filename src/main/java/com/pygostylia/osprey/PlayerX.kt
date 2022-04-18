package com.pygostylia.osprey

import com.pygostylia.osprey.packets.ClientBoundPacket
import com.pygostylia.osprey.packets.ClientBoundPacketID
import com.pygostylia.osprey.packets.ProtocolVersion
import java.io.ByteArrayOutputStream
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

class PlayerX(private val connection: Connection) : EntityX("minecraft:player", Collider(0.6F, 1.8F)) {
    enum class State {
        Status, Login, Play,
    }

    companion object {
        const val encryptionKey: String = "The encryption key"
    }

    var state: State = State.Status
    var name: String = "<none>"
    val ping: Duration = Duration.INFINITE
    var inventory: Inventory = Inventory()
    var selectedInventorySlot: Int = 0
    val loadedChunks: Set<ChunkPosition> = ConcurrentHashMap.newKeySet()
    val dispatchedChunks: Set<ChunkPosition> = ConcurrentHashMap.newKeySet()
    var renderDistance: Int = 10
    val editorBlockPositions: Array<BlockPosition?> = arrayOf(null, null)

    val protocolVersion = ProtocolVersion.V754

    private var isCreativeFlying: Boolean = false
    private var isElytraFlying: Boolean = false
    private var isFlying: Boolean = false
    private var isSprinting: Boolean = false
    private var isShielding: Boolean = false
    private var isSneaking: Boolean = false

    var vehicle: EntityX? = null

    fun sendPacket(p: Packet) = 0
    fun sendLoginSuccess() = 0

    fun handleConnection() {
        while (!connection.isClosed) {
            try {
                val p = connection.readPacket()
                handlePacket(p)
            } catch (e: SocketException) {
                println("Exception! ${e.message};  Last packet was ${connection.lastPacketType}")
                e.printStackTrace()
                connection.close()
            }
        }
    }

    private fun handlePacket(p: Packet) {
        when (state) {
            State.Status -> handleStatusPacket(p)
            State.Login -> handleLoginPacket(p)
            State.Play -> handlePlayPacket(p)
        }
    }

    private fun handleStatusPacket(p: Packet) {
        when (p.type) {
            // 0 -> handleHandshake(p)
            // 1 -> handlePing(p)
            else -> handleUnknownPacket(p)
        }
    }

    private fun handleLoginPacket(p: Packet) {
        when (p.type) {
            // 0 -> handleLoginStart(p)
            // 1 -> handleEncryptionResponse(p)
            else -> handleUnknownPacket(p)
        }
    }

    private val playPacketHandlers: HashMap<Int, Function<Unit>> = hashMapOf(
        0 to ::handleTeleportConfirm,
        1 to ::handleQueryBlockNBT,
        2 to ::handleSetDifficulty,
        3 to ::handleChatMessage,
        // 4 to ::handleClientStatus,
        // 5 to ::handleSettings,
        // 11 to ::handlePluginMessage,
        // 14 to ::handleInteractEntity,
        // 16 to ::handleKeepAlive,
        // 18 to ::handlePosition,
        19 to ::handlePositionAndRotation,
        // 20 to ::handleRotation,
        21 to ::handleMovement,
        // 22 to ::handleVehicleMove,
        // 23 to ::handleSteerBoat,
        // 26 to ::handleIsFlying,
        // 27 to ::handlePlayerAction,
        // 28 to ::handleEntityAction,
        // 29 to ::handleSteerVehicle,
        // 37 to ::handleHeldItemChange,
        // 40 to ::handleCreativeInventoryAction,
        // 44 to ::handleAnimation,
        // 46 to ::handlePlayerPlaceBlock,
        // 47 to ::handlePlayerUseItem,
    )

    private fun handlePlayPacket(p: Packet) {
        val handler = playPacketHandlers[p.type] ?: ::handleUnknownPacket
        handler.run {}
    }

    private fun handleUnknownPacket(p: Packet) {
        println("unknown packet ${p.type} for player $name")
    }

    private fun handleTeleportConfirm(p: Packet) {}
    private fun handleQueryBlockNBT(p: Packet) {}
    private fun handleSetDifficulty(p: Packet) {}
    private fun handleChatMessage(p: Packet) {
        val message = p.readString()
        BackgroundJob.queueHighPriority {
            Main.forEachPlayer { it.sendChat(asPlayer(), message) }
        }
    }

    private fun handleMovement(p: Packet) {
        BackgroundJob.queueHighPriority {
            Main.forEachPlayer { it.sendEntityTeleport(asPlayer().id, EntityPosition()) }
        }

        BackgroundJob.queue {
            // updateChunks()
        }
    }

    private fun handlePositionAndRotation(p: Packet) {
        val pos = p.readPosition()
    }

    private fun asPlayer(): Player {
        val p = Player(null)
        p.name = name
        return p
    }
}