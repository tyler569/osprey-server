package com.pygostylia.osprey

import com.pygostylia.osprey.BlockPosition.Companion.relativeLocation
import com.pygostylia.osprey.Main.forEachPlayer
import com.pygostylia.osprey.Main.playerByName
import com.pygostylia.osprey.Main.scheduler
import com.pygostylia.osprey.Main.world
import com.pygostylia.osprey.commands.Command
import com.pygostylia.osprey.commands.CommandAlias
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.*

object Commands {
    @Command(value = "teleport", args = ["destination: vec3"])
    @CommandAlias("tp")
    fun teleport(sender: Player, args: Array<String>) {
        if (args.size < 4) {
            sender.sendError("Not enough arguments")
            return
        }
        val destination = relativeLocation(
            sender.location(),
            Arrays.copyOfRange(args, 1, 4)
        )
        sender.teleport(destination)
    }

    @Command(value = "teleport", args = ["target: player"])
    fun teleportPlayer(sender: Player, args: Array<String?>) {
        if (args.size < 2) {
            sender.sendError("Not enough arguments")
            return
        }
        val target = playerByName(args[1]!!)
        if (target == null) {
            sender.sendError(String.format("%s is not online", args[1]))
        }
        assert(target != null)
        sender.teleport(target!!.location())
    }

    @Command("hello")
    fun hello(sender: Player, args: Array<String?>?) {
        sender.sendNotification("Hello World")
    }

    @Command("/pos1")
    @CommandAlias("/1")
    fun setPos1(sender: Player, args: Array<String?>?) {
        sender.setEditorLocation(0, sender.location())
    }

    @Command("/pos2")
    @CommandAlias("/2")
    fun setPos2(sender: Player, args: Array<String?>?) {
        sender.setEditorLocation(1, sender.location())
    }

    @Command("/sel")
    fun editorClear(sender: Player, args: Array<String?>?) {
        sender.unsetEditorSelection()
    }

    @Command(value = "/set", args = ["block: block_state"])
    fun editorSet(sender: Player, args: Array<String>) {
        val (x1, y1, z1) = sender.editorPositions[0]
        val (x2, y2, z2) = sender.editorPositions[1]
        val blockId: Int
        blockId = try {
            args[1].toInt()
        } catch (ignored: NumberFormatException) {
            val state = BlockState(args[1])
            state.protocolId().toInt()
        }
        var count = 0
        for (y in Integer.min(y1, y2)..Integer.max(y1, y2)) {
            for (z in Integer.min(z1, z2)..Integer.max(z1, z2)) {
                for (x in Integer.min(x1, x2)..Integer.max(x1, x2)) {
                    val location = BlockPosition(x, y, z)
                    world.setBlock(location, blockId)
                    count++
                    forEachPlayer { player: Player ->
                        player.sendBlockChange(
                            location,
                            blockId
                        )
                    }
                }
            }
        }
        sender.sendEditorNotification(String.format("Set %s blocks", count))
    }

    @Command("lag")
    fun lag(sender: Player, args: Array<String?>?) {
        forEachPlayer { player: Player ->
            player.sendNotification(
                String.format(
                    "%s thought there was some lag",
                    sender.name()
                )
            )
        }
        sender.kick()
    }

    @Command(value = "speed", args = ["speed: float"])
    fun speed(sender: Player, args: Array<String>) {
        val speed = args[1].toFloat()
        sender.sendSpeed(speed)
    }

    @Command("save")
    fun save(sender: Player, args: Array<String?>?) {
        val now = Instant.now()
        if (sender.isAdmin) world.save()
        val then = Instant.now()
        val took = Duration.between(now, then)
        sender.sendNotification(
            String.format(
                "Saved world! (%fms)",
                took.nano.toDouble() / 1000000
            )
        )
    }

    @Command(value = "gamemode", args = ["mode: integer(0,3)"])
    @CommandAlias("gm")
    fun gamemode(sender: Player, args: Array<String>) {
        if (args.size < 2) {
            sender.sendError("Not enough arguments")
            return
        }
        val value = args[1].toInt()
        sender.changeGamemode(value)
    }

    @Command(value = "gamestate", args = ["mode: integer(0,14)", "arg: float"])
    @CommandAlias("gs")
    fun gameState(sender: Player?, args: Array<String>) {
        val reason = args[1].toByte()
        val value = args[2].toFloat()
        forEachPlayer { player: Player -> player.sendChangeGameState(reason.toInt(), value) }
    }

    @Command("falling")
    fun falling(sender: Player, args: Array<String?>?) {
        sender.placeFalling = sender.placeFalling xor true
    }

    @Command("boom")
    fun boom(sender: Player, args: Array<String?>?) {
        sender.boom = sender.boom xor true
    }

    @Command("bullettime")
    fun bulletTime(sender: Player, args: Array<String?>?) {
        sender.bulletTime = sender.bulletTime xor true
    }

    @Command("spawn")
    @Throws(IOException::class)
    fun spawn(sender: Player, args: Array<String?>?) {
        sender.teleport(BlockPosition(0, 32, 0))
    }

    @Command(value = "entitystatus", args = ["id: integer", "status: integer(0,255)"])
    @CommandAlias("es")
    fun entityStatus(sender: Player?, args: Array<String>) {
        forEachPlayer { player: Player -> player.sendEntityStatus(args[1].toInt(), args[2].toByte()) }
    }

    @Command("cloud")
    fun cloud(sender: Player, args: Array<String?>?) {
        val future = scheduler.submitForEachTick {
            forEachPlayer { player: Player ->
                player.sendEntityStatus(
                    sender.id(),
                    43.toByte()
                )
            }
        }
        sender.addFuture(future)
    }

    @Command(value = "cancel", args = ["job: integer"])
    fun cancel(sender: Player, args: Array<String>) {
        val job = args[1].toInt()
        val future = sender.removeFuture(job)
        future.cancel(false)
        sender.sendNotification("Job $job cancelled")
    }

    @Command("followlightining")
    fun followLightning(sender: Player, args: Array<String?>?) {
        val future = scheduler.submitForEachTick {
            val entityPosition: EntityPosition
            entityPosition = if (sender.isSneaking()) {
                sender.position().offset(0.0, 1.5, 0.0)
            } else {
                sender.position().offset(0.0, 1.8, 0.0)
            }
            val bolt = LightningEntity(entityPosition)
            forEachPlayer { player: Player -> player.sendSpawnEntity(bolt) }
            bolt.destroy()
        }
        sender.addFuture(future)
    }

    @Command(value = "kick", args = ["target: player"])
    fun kick(sender: Player, args: Array<String>) {
        val target = playerByName(args[1])
        if (target == null) {
            sender.sendError(args[1] + " is not online")
            return
        }
        target.kick()
    }
}