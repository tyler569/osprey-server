package com.pygostylia.osprey;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/*

The idea of CommandBucket is that it's a big bucket with commands in it.

It stores commands in two representations, as a flat array and as a tree. The
tree matches what Brigadier is expecting to see in its packets closely.

Commands are defined with an @Command annotation, whose format is the "value"
parameter is the name of the command (comes after the '/'), and the "args"
parameter are string-encoded argument names and types for brigadier.
The names and types are parsed by CommandParameter::fromArg into CommandParameters

CommandParameter is the main building block type for the in-memory tree. It
represents one notional element of a command invocation. This could be one
argument, as in a String, or multiple as in a Vec3. It could even represent no
arguments at all, as it does in the case of the root node.

The tree is used for both brigadier packet generation and also for dispatching
commands from users. When a user runs a command, it is sent to

The tree is used for both brigadier packet generation and also for dispatching
commands from users. When a user runs a command, it is sent to
CommandBucket::findMatch which recurses through the tree until if finds a terminal
node that represents the users command. The original method that was annotated
is attached to a CommandParameter marked isTerminal at the leaves of the tree,
and if one is found for a given query, it is executed.

 */

public class CommandBucket {
    static class FlatCommand {
        List<CommandParameter> parameters;
        List<String> aliases;
        Method method;

        FlatCommand(String name, List<CommandParameter> args, Method method) {
            aliases = new ArrayList<>();
            this.method = method;
            parameters = args;
            parameters.add(0, new CommandParameter(name));
        }
    }

    ArrayList<FlatCommand> flatCommands = new ArrayList<>();

    static class CommandElement {
        CommandParameter parameter;
        boolean isRedirect;
        boolean isTerminal;
        boolean isRoot;
        CommandElement redirect; // if is redirect
        Method method;           // if is terminal

        Integer index; // in the brigadier packet

        // tree
        ArrayList<CommandElement> children = new ArrayList<>();

        static CommandElement rootNode() {
            var ce = new CommandElement();
            ce.parameter = new CommandParameter("");
            ce.isRoot = true;
            return ce;
        }

        static CommandElement alias(CommandElement node, String alias) {
            var ce = new CommandElement();
            ce.parameter = new CommandParameter(alias);
            ce.isRedirect = true;
            ce.redirect = node;
            return ce;
        }

        boolean isLiteral() {
            return !isRoot && parameter.type == CommandParameter.Type.Literal;
        }

        boolean isArgument() {
            return parameter.type != CommandParameter.Type.Literal;
        }

        @Override
        public String toString() {
            return String.format("Node(%s, terminal: %s, %s)", parameter, isTerminal, children);
        }
    }

    CommandElement rootNode;

    CommandBucket() {
        rootNode = CommandElement.rootNode();
        for (Method method : CommandBucket.class.getDeclaredMethods()) {
            FlatCommand flatCommand;
            if (method.isAnnotationPresent(Command.class)) {
                Command commandInfo = method.getAnnotation(Command.class);
                var args = Arrays.stream(commandInfo.args()).map(CommandParameter::fromArg).collect(Collectors.toList());
                flatCommand = new FlatCommand(commandInfo.value(), args, method);
                flatCommands.add(flatCommand);
            } else {
                continue;
            }
            for (CommandAlias commandAlias: method.getAnnotationsByType(CommandAlias.class)) {
                String alias = commandAlias.value();
                flatCommand.aliases.add(alias);
            }
        }
    }

    CommandElement findMatch(CommandElement node, String[] args, int offset, boolean override) {
        // System.out.println("match " + node + " " + Arrays.toString(args) + " " + offset);
        if (offset == args.length && node.isTerminal) {
            return node;
        }
        for (var child : node.children) {
            var n = child.parameter.matches(args, offset);
            if (n == 0) continue;
            CommandElement found;
            if (child.isRedirect) {
                found = findMatch(child.redirect, args, offset + n, true);
            } else {
                found = findMatch(child, args, offset + n, false);
            }
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    void dispatch(Player sender, String[] args) throws IOException {
        CommandElement element;
        Method method;
        try {
            element = findMatch(rootNode, args, 0, false);
        } catch (Exception e) {
            sender.sendError("An error occurred finding that command: " + e);
            return;
        }
        if (element == null) {
            sender.sendError(String.format("Invalid command: '%s'", args[0]));
            return;
        }
        method = element.method;
        try {
            method.invoke(this, sender, (Object) args);
        } catch (Exception e) {
            var cause = e.getCause();
            if (cause != null) {
                sender.sendError("Error: " + cause.getMessage());
            } else {
                sender.sendError("Error: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    CommandElement insertBrigadier(CommandElement root, List<CommandParameter> parameters, int offset, Method method) {
        for (var child : root.children) {
            if (child.parameter.equals(parameters.get(offset))) {
                if (parameters.size() == offset + 1) {
                    child.isTerminal = true;
                    return child;
                }
                insertBrigadier(child, parameters, offset + 1, method);
                return child;
            }
        }
        CommandElement first = null;
        var last = root;
        for (int i = offset; i < parameters.size(); i++) {
            var ce = new CommandElement();
            if (first == null) first = ce;
            ce.parameter = parameters.get(i);
            ce.isTerminal = i == parameters.size() - 1;
            ce.method = ce.isTerminal ? method : null;
            last.children.add(ce);
            last = ce;
        }
        return first;
    }

    int encodeBrigadier(int index, int depth, CommandElement node, PacketBuilder p) throws IOException {
        for (var child : node.children) {
            index = encodeBrigadier(index, depth + 1, child, p);
        }
        byte flags = 0;
        if (node.isLiteral() || node.isRedirect) {
            flags = 1;
        } else if (node.isArgument()) {
            flags = 2;
        }
        if (node.isTerminal) {
            flags |= 0x04;
        }
        if (node.isRedirect) {
            flags |= 0x08;
        }
        p.writeByte(flags);
        p.writeVarInt(node.children.size());
        for (var child : node.children) {
            p.writeVarInt(child.index);
        }
        if (node.isRedirect) {
            p.writeVarInt(node.redirect.index);
        }
        if (!node.isRoot) {
            p.writeString(node.parameter.literalValue);
        }
        if (node.isArgument()) {
            p.writeString(node.parameter.brigadierType());

            switch (node.parameter.type) {
                case Integer -> {
                    if (node.parameter.min != null) {
                        p.write(0x03);
                        p.writeInt(node.parameter.min);
                        p.writeInt(node.parameter.max);
                    } else {
                        p.write(0);
                    }
                }
                case Float -> {
                    if (node.parameter.min != null) {
                        p.write(0x03);
                        p.writeFloat(node.parameter.min);
                        p.writeFloat(node.parameter.max);
                    } else {
                        p.write(0);
                    }
                }
                case String -> p.writeVarInt(0); // SINGLE_WORD
                case Player -> p.write(0x03); // single entity + player
                case Entity -> p.write(0x01); // single entity
            }
        }
        node.index = index;
        return index + 1;
    }

    int encodeBrigadier(PacketBuilder p) throws IOException {
        for (var flatCommand : flatCommands) {
            var node = insertBrigadier(rootNode, flatCommand.parameters, 0, flatCommand.method);

            for (var alias : flatCommand.aliases) {
                rootNode.children.add(CommandElement.alias(node, alias));
            }
        }
        // System.out.println(rootNode);
        return encodeBrigadier(0, 0, rootNode, p);
    }

    byte[] brigadierPacket() throws IOException {
        PacketBuilder p = new PacketBuilder();
        var tmp = new PacketBuilder();
        var length = Main.commands.encodeBrigadier(tmp);

        p.writeVarInt(length);
        p.write(tmp.toByteArray());
        p.writeVarInt(length - 1);
        return p.toByteArray();
    }



    // todo: move to a subclass or something


    @Command(value = "teleport", args = {"destination: vec3"})
    @CommandAlias("tp")
    void teleport(Player sender, String[] args) throws IOException {
        if (args.length < 4) {
            sender.sendError("Not enough arguments");
            return;
        }
        Location destination = Location.relativeLocation(
            sender.position.location(),
            Arrays.copyOfRange(args, 1, 4)
        );
        sender.teleport(destination);
    }

    @Command(value = "teleport", args = {"destination: player"})
    void teleportPlayer(Player sender, String[] args) throws IOException {
        if (args.length < 2) {
            sender.sendError("Not enough arguments");
            return;
        }
        Player target = Main.playerByName(args[1]);
        if (target == null) {
            sender.sendError(String.format("%s is not online", args[1]));
        }
        assert target != null;
        sender.teleport(target.position.location());
    }

    @Command("hello")
    void hello(Player sender, String[] args) throws IOException {
        sender.sendNotification("Hello World");
    }

    @Command("/pos1")
    @CommandAlias("/1")
    void setPos1(Player sender, String[] args) throws IOException {
        sender.setEditorLocation(0, sender.position.location());
    }

    @Command("/pos2")
    @CommandAlias("/2")
    void setPos2(Player sender, String[] args) throws IOException {
        sender.setEditorLocation(1, sender.position.location());
    }

    @Command("/sel")
    void editorClear(Player sender, String[] args) throws IOException {
        sender.unsetEditorSelection();
    }

    @Command(value = "/set", args = {"block: string"})
    void editorSet(Player sender, String[] args) throws IOException {
        var l1 = sender.editorLocations[0];
        var l2 = sender.editorLocations[1];
        int blockId;
        try {
            blockId = Integer.parseInt(args[1]);
        } catch (NumberFormatException ignored) {
            blockId = Main.blockDefaultId(args[1]);
        }
        int count = 0;
        for (int y = Integer.min(l1.y(), l2.y()); y <= Integer.max(l1.y(), l2.y()); y++) {
            for (int z = Integer.min(l1.z(), l2.z()); z <= Integer.max(l1.z(), l2.z()); z++) {
                for (int x = Integer.min(l1.x(), l2.x()); x <= Integer.max(l1.x(), l2.x()); x++) {
                    var location = new Location(x, y, z);
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
    void lag(Player sender, String[] args) throws IOException {
        Main.forEachPlayer((player) -> {
            player.sendNotification(String.format("%s thought there was some lag", sender.name));
        });
        sender.kick();
    }

    @Command(value = "speed", args = {"speed: float"})
    void speed(Player sender, String[] args) throws IOException {
        var speed = Float.parseFloat(args[1]);
        sender.sendSpeed(speed);
    }

    @Command("save")
    void save(Player sender, String[] args) throws IOException {
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
    void gamemode(Player sender, String[] args) throws IOException {
        if (args.length < 2) {
            sender.sendError("Not enough arguments");
            return;
        }
        var value = Integer.parseInt(args[1]);
        sender.changeGamemode(value);
    }

    @Command(value = "gamestate", args = {"mode: integer(0,14)", "arg: float"})
    @CommandAlias("gs")
    void gameState(Player sender, String[] args) throws IOException {
        var reason = Byte.parseByte(args[1]);
        var value = Float.parseFloat(args[2]);
        Main.forEachPlayer((player) -> {
            player.sendChangeGameState(reason, value);
        });
    }

}
