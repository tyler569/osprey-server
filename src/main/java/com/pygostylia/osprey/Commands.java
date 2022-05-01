package com.pygostylia.osprey;

import com.pygostylia.osprey.commands.Command;
import com.pygostylia.osprey.commands.CommandAlias;
import com.pygostylia.osprey.entities.LightningEntity;
import com.pygostylia.osprey.entities.Player;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

public class Commands {
    @Command(value = "teleport", args = {"destination: vec3"})
    @CommandAlias("tp")
    public static void teleport(Player sender, String[] args) {
        if (args.length < 4) {
            sender.sendError("Not enough arguments");
            return;
        }
        BlockPosition destination = BlockPosition.relativeLocation(
                sender.location(),
                Arrays.copyOfRange(args, 1, 4)
        );
        sender.teleport(destination);
    }

    @Command(value = "teleport", args = {"target: player"})
    public static void teleportPlayer(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendError("Not enough arguments");
            return;
        }
        Player target = Main.playerByName(args[1]);
        if (target == null) {
            sender.sendError(String.format("%s is not online", args[1]));
        }
        assert target != null;
        sender.teleport(target.location());
    }

    @Command("hello")
    public static void hello(Player sender, String[] args) {
        sender.sendNotification("Hello World");
    }

    @Command("/pos1")
    @CommandAlias("/1")
    public static void setPos1(Player sender, String[] args) {
        sender.setEditorLocation(0, sender.location());
    }

    @Command("/pos2")
    @CommandAlias("/2")
    public static void setPos2(Player sender, String[] args) {
        sender.setEditorLocation(1, sender.location());
    }

    @Command("/sel")
    public static void editorClear(Player sender, String[] args) {
        sender.unsetEditorSelection();
    }

    @Command(value = "/set", args = {"block: block_state"})
    public static void editorSet(Player sender, String[] args) {
        var l1 = sender.getEditorLocations()[0];
        var l2 = sender.getEditorLocations()[1];
        int blockId;
        try {
            blockId = Integer.parseInt(args[1]);
        } catch (NumberFormatException ignored) {
            var state = new BlockState(args[1]);
            blockId = state.protocolId();
        }
        int count = 0;
        for (int y = Integer.min(l1.y(), l2.y()); y <= Integer.max(l1.y(), l2.y()); y++) {
            for (int z = Integer.min(l1.z(), l2.z()); z <= Integer.max(l1.z(), l2.z()); z++) {
                for (int x = Integer.min(l1.x(), l2.x()); x <= Integer.max(l1.x(), l2.x()); x++) {
                    var location = new BlockPosition(x, y, z);
                    Main.world.setBlock(location, blockId);
                    count++;
                    int finalBlockId = blockId;
                    Main.forEachPlayer((player) -> {
                        player.sendBlockChange(location, finalBlockId);
                    });
                }
            }
        }
        sender.sendEditorNotification(String.format("Set %s blocks", count));
    }

    @Command("lag")
    public static void lag(Player sender, String[] args) {
        Main.forEachPlayer((player) -> {
            player.sendNotification(String.format("%s thought there was some lag", sender.name()));
        });
        sender.kick();
    }

    @Command(value = "speed", args = {"speed: float"})
    public static void speed(Player sender, String[] args) {
        var speed = Float.parseFloat(args[1]);
        sender.sendSpeed(speed);
    }

    @Command("save")
    public static void save(Player sender, String[] args) {
        var now = Instant.now();
        if (sender.isAdmin())
            Main.world.save();
        var then = Instant.now();
        var took = Duration.between(now, then);
        sender.sendNotification(String.format("Saved world! (%fms)",
                (double) took.getNano() / 1000000));
    }

    @Command(value = "gamemode", args = {"mode: integer(0,3)"})
    @CommandAlias("gm")
    public static void gamemode(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendError("Not enough arguments");
            return;
        }
        var value = Integer.parseInt(args[1]);
        sender.changeGamemode(value);
    }

    @Command(value = "gamestate", args = {"mode: integer(0,14)", "arg: float"})
    @CommandAlias("gs")
    public static void gameState(Player sender, String[] args) {
        var reason = Byte.parseByte(args[1]);
        var value = Float.parseFloat(args[2]);
        Main.forEachPlayer((player) -> {
            player.sendChangeGameState(reason, value);
        });
    }

    @Command("falling")
    public static void falling(Player sender, String[] args) {
        sender.placeFalling ^= true;
    }

    @Command("boom")
    public static void boom(Player sender, String[] args) {
        sender.boom ^= true;
    }

    @Command("bullettime")
    public static void bulletTime(Player sender, String[] args) {
        sender.bulletTime ^= true;
    }

    @Command("spawn")
    public static void spawn(Player sender, String[] args) throws IOException {
        sender.teleport(new BlockPosition(0, 32, 0));
    }

    @Command(value = "entitystatus", args = {"id: integer", "status: integer(0,255)"})
    @CommandAlias("es")
    public static void entityStatus(Player sender, String[] args) {
        Main.forEachPlayer(player -> {
            player.sendEntityStatus(Integer.parseInt(args[1]), Byte.parseByte(args[2]));
        });
    }

    @Command("cloud")
    public static void cloud(Player sender, String[] args) {
        var future = Main.scheduler.submitForEachTick(() -> {
            Main.forEachPlayer(player -> player.sendEntityStatus(sender.id(), (byte) 43));
        });
        sender.addFuture(future);
    }

    @Command(value = "cancel", args = "job: integer")
    public static void cancel(Player sender, String[] args) {
        int job = Integer.parseInt(args[1]);
        var future = sender.removeFuture(job);
        future.cancel(false);
        sender.sendNotification("Job " + job + " cancelled");
    }

    @Command("followlightining")
    public static void followLightning(Player sender, String[] args) {
        var future = Main.scheduler.submitForEachTick(() -> {
            EntityPosition entityPosition;
            if (sender.isSneaking()) {
                entityPosition = sender.position().offset(0, 1.5, 0);
            } else {
                entityPosition = sender.position().offset(0, 1.8, 0);
            }
            var bolt = new LightningEntity(entityPosition);
            Main.forEachPlayer(player -> player.sendSpawnEntity(bolt));
            bolt.destroy();
        });
        sender.addFuture(future);
    }

    @Command(value = "kick", args = {"target: player"})
    public static void kick(Player sender, String[] args) {
        Player target = Main.playerByName(args[1]);
        if (target == null) {
            sender.sendError(args[1] + " is not online");
            return;
        }
        target.kick();
    }
}
