package io.philbrick.minecraft;

import jdk.jshell.spi.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

public class CommandBucket {
    static class FlatCommand {
        List<CommandParameter> parameters;
        Method method;

        FlatCommand(String name, List<CommandParameter> args, Method method) {
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

        // tree
        ArrayList<CommandElement> children = new ArrayList<>();

        static CommandElement rootNode() {
            var ce = new CommandElement();
            ce.parameter = new CommandParameter("");
            ce.isRoot = true;
            return ce;
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
            if (method.isAnnotationPresent(Command.class)) {
                Command commandInfo = method.getAnnotation(Command.class);
                var args = Arrays.stream(commandInfo.args()).map(CommandParameter::fromArg).collect(Collectors.toList());
                var flatCommand = new FlatCommand(commandInfo.name(), args, method);
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

    void insertBrigadier(CommandElement root, List<CommandParameter> parameters, int offset, Method method) {
        for (var child : root.children) {
            if (child.parameter.matches(parameters.get(offset))) {
                if (parameters.size() == offset + 1) {
                    child.isTerminal = true;
                    return;
                }
                insertBrigadier(child, parameters, offset + 1, method);
                return;
            }
        }
        var last = root;
        for (int i = offset; i < parameters.size(); i++) {
            var ce = new CommandElement();
            ce.parameter = parameters.get(i);
            ce.isTerminal = i == parameters.size() - 1;
            ce.method = ce.isTerminal ? method : null;
            last.children.add(ce);
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


    @Command(name = "tp", args = {"destination: vec3"})
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

    @Command(name = "tp", args = {"destination: player"})
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
