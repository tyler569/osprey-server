package io.philbrick.minecraft;

import jdk.jshell.spi.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class CommandBucket {
    static class FlatCommand {
        Object[] parameters;
        Method method;

        FlatCommand(String name, Object[] args, Method method) {
            this.method = method;
            parameters = new Object[args.length + 1];
            parameters[0] = name;
            System.arraycopy(args, 0, parameters, 1, args.length);
        }
    }

    ArrayList<FlatCommand> flatCommands = new ArrayList<>();

    static class CommandElement {
        Object parameter;        // if String, literal. if ParameterType, argument
        boolean isTerminal;
        boolean isRoot;
        CommandElement redirect; // if is redirect
        Method method;           // if is terminal

        // tree
        ArrayList<CommandElement> children = new ArrayList<>();
        CommandElement parent;

        static CommandElement rootNode() {
            var ce = new CommandElement();
            ce.parameter = "";
            ce.isRoot = true;
            return ce;
        }

        @Override
        public String toString() {
            return String.format("Node(%s, terminal: %s, %s)", parameter, isTerminal, children);
        }
    }

    CommandElement rootNode = CommandElement.rootNode();

    CommandBucket() {
        for (Method method : CommandBucket.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command commandInfo = method.getAnnotation(Command.class);
                var flatCommand = new FlatCommand(commandInfo.name(), commandInfo.args(), method);
                flatCommands.add(flatCommand);
            }
        }
    }

    void dispatch(Player sender, String[] args) throws IOException {
        try {
            throw new ExecutionControl.NotImplementedException("lol");
        } catch (Exception e) {
            sender.sendError(e.toString());
        }
    }

    void insertBrigadier(CommandElement root, Object[] parameters, int offset, Method method) {
        for (var child : root.children) {
            if (child.parameter.equals(parameters[offset])) {
                if (parameters.length == offset + 1) {
                    child.isTerminal = true;
                    return;
                }
                insertBrigadier(child, parameters, offset + 1, method);
                return;
            }
        }
        var last = root;
        for (int i = offset; i < parameters.length; i++) {
            var ce = new CommandElement();
            ce.parameter = parameters[i];
            ce.isTerminal = i == parameters.length - 1;
            ce.method = ce.isTerminal ? method : null;
            ce.parent = last;
            ce.parent.children.add(ce);
            last = ce;
        }
    }

    void encodeBrigadier(PacketBuilder p) throws IOException {
        for (var flatCommand : flatCommands) {
            insertBrigadier(rootNode, flatCommand.parameters, 0, flatCommand.method);
        }
        System.out.println(rootNode);
    }



    // todo: move to a subclass or something


    @Command(name = "tp", args = {Command.ParameterType.Vec3})
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

    @Command(name = "tp", args = {Command.ParameterType.Player})
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

    @Command(name = "hello")
    void hello(Player sender, String[] args) throws IOException {
        sender.sendNotification("Hello World");
    }
}
