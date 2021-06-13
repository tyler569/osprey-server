package com.pygostylia.osprey;

import com.pygostylia.osprey.nbt.NBTCompound;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandBucket2 {
    static abstract class CommandParameter {
        String name;

        CommandParameter(String name) {
            this.name = name;
        }

        static Pattern pattern = Pattern.compile("<(\\w+)(?::(\\S+))?>");

        static CommandParameter fromString(String value, Parameter parameter) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                String name = matcher.group(1);
                String typeDesc = matcher.group(2);
                Class<?> argumentType = parameter.getType();

                if (argumentType == Integer.class) {
                    return new IntegerParameter(name, typeDesc);
                } else if (argumentType == Float.class) {
                    return new FloatParameter(name, typeDesc);
                } else if (argumentType == Boolean.class) {
                    return new BooleanParameter(name);
                } else if (argumentType == String.class) {
                    return new StringParameter(name);
                } else if (argumentType == Entity.class) {
                    return new EntityParameter(name);
                } else if (argumentType == Player.class) {
                    return new PlayerParameter(name);
                } else if (argumentType == BlockState.class) {
                    return new BlockStateParameter(name);
                } else if (argumentType == Location.class) {
                    return new LocationParameter(name);
                } else if (argumentType == NBTCompound.class) {
                    return new NBTParameter(name);
                } else {
                    throw new IllegalStateException(argumentType + " cannot be a command argument");
                }
            } else {
                return new LiteralParameter(value);
            }

        }

        abstract String brigadierType();
    }

    static class BooleanParameter extends CommandParameter {
        BooleanParameter(String name) {
            super(name);
        }

        String brigadierType() {
            return "brigadier:bool";
        }
    }

    static class FloatParameter extends CommandParameter {
        Float max, min;

        FloatParameter(String name, String typeDesc) {
            super(name);
        }

        String brigadierType() {
            return "brigadier:float";
        }
    }

    static class IntegerParameter extends CommandParameter {
        Integer max, min;

        IntegerParameter(String name, String typeDesc) {
            super(name);
        }

        String brigadierType() {
            return "brigadier:integer";
        }
    }

    static class StringParameter extends CommandParameter {
        StringParameter(String name) {
            super(name);
        }
        // TODO: enum { OneWord, Quotes, RestOfLine }

        String brigadierType() {
            return "brigadier:string";
        }
    }

    static class EntityParameter extends CommandParameter {
        EntityParameter(String name) {
            super(name);
        }

        String brigadierType() {
            return "minecraft:entity";
        }
    }

    static class PlayerParameter extends CommandParameter {
        PlayerParameter(String name) {
            super(name);
        }

        String brigadierType() {
            return "minecraft:entity";
        }
    }

    static class LocationParameter extends CommandParameter {
        LocationParameter(String name) {
            super(name);
        }

        String brigadierType() {
            return "minecraft:vec3";
        }
    }

    static class BlockStateParameter extends CommandParameter {
        BlockStateParameter(String name) {
            super(name);
        }

        String brigadierType() {
            return "minecraft:block_state";
        }
    }

    static class NBTParameter extends CommandParameter {
        NBTParameter(String name) {
            super(name);
        }

        String brigadierType() {
            return "minecraft:nbt";
        }
    }

    static class LiteralParameter extends CommandParameter {
        public LiteralParameter(String value) {
            super(value);
        }

        String brigadierType() {
            return null;
        }
    }

    static abstract class Node {
        String name;
        Integer index;
        List<Node> children = new ArrayList<>();
    }

    static class RootNode extends Node {
        RootNode() {
            name = null;
        }
    }

    static class LiteralNode extends Node {
        LiteralNode(String name) {
            this.name = name;
        }
    }

    static class ArgumentNode extends Node {
        Parameter parameterInfo;

        ArgumentNode(String name, Parameter parameterInfo) {
            this.name = name;
            this.parameterInfo = parameterInfo;
        }
    }

    static class RedirectNode extends Node {
        Node redirect;

        RedirectNode(String name, Node redirectTo) {
            this.name = name;
            this.redirect = redirectTo;
        }
    }


    static ArrayList<CommandParameter> assembleParameters(String[] arguments, Queue<Parameter> parameters) {
        var result = new ArrayList<CommandParameter>();
        var parameter = parameters.remove();
        for (var argument : arguments) {
            var commandParameter = CommandParameter.fromString(argument, parameter);
            if (!(commandParameter instanceof LiteralParameter)) {
                parameter = parameters.remove();
            }
            result.add(commandParameter);
        }
        return result;
    }


    RootNode rootNode;

    static {
        for (Method method : CommandBucket2.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Command2.class)) {
                Command2 commandInfo = method.getAnnotation(Command2.class);
                String[] args = commandInfo.value().split(" +");
                Queue<Parameter> parameters = new LinkedList<>(Arrays.asList(method.getParameters()));
                ArrayList<CommandParameter> commandParameters = assembleParameters(args, parameters);
            }
        }
    }


    @Command2("/tp <player>")
    void teleportPlayer(Player sender, Player target) {
        sender.teleport(target.location());
    }

    @Command2("/tp <location>")
    void teleportLocation(Player sender, Location location) {
        sender.teleport(location);
    }
}
