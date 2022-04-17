package com.pygostylia.osprey.commands;

import com.pygostylia.osprey.*;

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
    byte[] packet;

    public CommandBucket() throws IOException {
        rootNode = CommandElement.rootNode();
        packet = brigadierPacket();
    }

    public void register(Class<?> commandClass) throws IOException {
        for (Method method : commandClass.getDeclaredMethods()) {
            FlatCommand flatCommand;
            if (method.isAnnotationPresent(Command.class)) {
                Command commandInfo = method.getAnnotation(Command.class);
                var args = Arrays.stream(commandInfo.args()).map(CommandParameter::fromArg).collect(Collectors.toList());
                flatCommand = new FlatCommand(commandInfo.value(), args, method);
                flatCommands.add(flatCommand);
            } else {
                continue;
            }
            for (CommandAlias commandAlias : method.getAnnotationsByType(CommandAlias.class)) {
                String alias = commandAlias.value();
                flatCommand.aliases.add(alias);
            }
        }
        packet = brigadierPacket();
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

    public void dispatch(Player sender, String[] args) {
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
            method.invoke(Commands.INSTANCE, sender, args);
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
        var length = encodeBrigadier(tmp);

        p.writeVarInt(length);
        p.write(tmp.toByteArray());
        p.writeVarInt(length - 1);
        return p.toByteArray();
    }

    public byte[] getPacket() {
        return packet;
    }
}
