package com.pygostylia.osprey.commands;

import com.pygostylia.osprey.BlockState;
import com.pygostylia.osprey.Entity;
import com.pygostylia.osprey.Location;
import com.pygostylia.osprey.Player;

import java.lang.reflect.Parameter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParameter {
    enum Type {
        Boolean,
        Float,
        Integer,
        String,
        Entity,
        Player,
        Vec2,
        Vec3,
        BlockState,
        NBT,
        Literal,
    }

    Type type;
    String literalValue;
    Integer min;
    Integer max;
    byte flags;

    Integer brigadierIndex;

    CommandParameter(Type type) {
        this.type = type;
    }

    CommandParameter(String literalValue) {
        this.type = Type.Literal;
        this.literalValue = literalValue;
    }

    String brigadierType() {
        return switch (type) {
            case Boolean -> "brigadier:bool";
            case Float -> "brigadier:float";
            case Integer -> "brigadier:integer";
            case String -> "brigadier:string";
            case Entity, Player -> "minecraft:entity";
            case Vec2 -> "minecraft:vec2";
            case Vec3 -> "minecraft:vec3";
            case BlockState -> "minecraft:block_state";
            case NBT -> "minecraft:nbt";
            case Literal -> null;
        };
    }

    static Pattern argRegex = Pattern.compile("(\\w+)(?::\s+(\\w+)(?:\\((.*)\\))?)?");

    static CommandParameter fromArg(String arg) {
        Matcher argMatcher = argRegex.matcher(arg);
        if (!argMatcher.find()) {
            throw new IllegalStateException("arg must match the regex: " + arg);
        }
        var name = argMatcher.group(1);
        var type = argMatcher.group(2);
        var argumentsString = argMatcher.group(3);
        String[] arguments = null;
        if (argumentsString != null) {
            arguments = argumentsString.split(", ?");
        }

        CommandParameter pt = new CommandParameter(Type.Literal);
        pt.literalValue = name;

        if (type == null) {
            return pt;
        }

        pt.type = switch (type.toLowerCase()) {
            case "boolean", "bool" -> Type.Boolean;
            case "float", "double", "real" -> Type.Float;
            case "integer", "int" -> Type.Integer;
            case "string", "str" -> Type.String;
            case "entity" -> Type.Entity;
            case "player" -> Type.Player;
            case "vec2", "vector2" -> Type.Vec2;
            case "vec3", "vector3" -> Type.Vec3;
            case "block", "block_state" -> Type.BlockState;
            case "nbt" -> Type.NBT;
            default -> throw new IllegalStateException("Unexpected value: " + type.toLowerCase());
        };

        switch (pt.type) {
            case Float, Integer -> {
                if (arguments != null) {
                    pt.min = java.lang.Integer.parseInt(arguments[0]);
                    pt.max = java.lang.Integer.parseInt(arguments[1]);
                }
            }
            case Player -> pt.flags = 0x03;
            case Entity -> pt.flags = 0x01;
        }

        return pt;
    }

    static CommandParameter fromClass(Parameter argument) {
        Class<?> argumentType = argument.getType();
        Type type;
        byte flags = 0;
        if (argumentType == Integer.class) {
            type = Type.Integer;
        } else if (argumentType == Float.class) {
            type = Type.Float;
        } else if (argumentType == Boolean.class) {
            type = Type.Boolean;
        } else if (argumentType == String.class) {
            type = Type.String;
        } else if (argumentType == Entity.class) {
            type = Type.Entity;
            flags = 0x01;
        } else if (argumentType == Player.class) {
            type = Type.Entity;
            flags = 0x03;
        } else if (argumentType == BlockState.class) {
            type = Type.BlockState;
        } else if (argumentType == Location.class) {
            type = Type.Vec3;
        } else {
            throw new IllegalStateException(argumentType + " cannot be a command argument");
        }
        CommandParameter pt = new CommandParameter(type);
        pt.literalValue = argument.getName();
        pt.flags = flags;
        return pt;
    }

    public boolean equals(CommandParameter other) {
        if (type == other.type) {
            if (type == Type.Literal) {
                return literalValue.equals(other.literalValue);
            } else if (type == Type.Integer || type == Type.Float) {
                return min.equals(other.min) && max.equals(other.max);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static boolean isInteger(String s, Integer min, Integer max) {
        try {
            var value = Integer.parseInt(s);
            if (min == null || max == null) return true;
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isFloat(String s, Integer min, Integer max) {
        try {
            var value = Float.parseFloat(s);
            if (min == null || max == null) return true;
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isRelativeInteger(String s) {
        if (s.startsWith("~")) {
            s = s.substring(1);
            if (s.equals("")) {
                return true;
            }
        }
        try {
            Float.parseFloat(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public int matches(String[] args, int offset) {
        if (args.length == offset) {
            return 0;
        }

        switch (type) {
            case Boolean -> {
                if (
                        args[offset].equalsIgnoreCase("true") ||
                                args[offset].equalsIgnoreCase("false")
                ) {
                    return 1;
                } else {
                    return 0;
                }
            }
            case Integer -> {
                return isInteger(args[offset], min, max) ? 1 : 0;
            }
            case Float -> {
                return isFloat(args[offset], min, max) ? 1 : 0;
            }
            case String, Player, Entity, NBT, BlockState -> {
                return 1;
            }
            case Vec2 -> {
                if (offset + 2 > args.length) {
                    return 0;
                }
                return isRelativeInteger(args[offset]) &&
                        isRelativeInteger(args[offset + 1]) ? 2 : 0;
            }
            case Vec3 -> {
                if (offset + 3 > args.length) {
                    return 0;
                }
                return isRelativeInteger(args[offset]) &&
                        isRelativeInteger(args[offset + 1]) &&
                        isRelativeInteger(args[offset + 2]) ? 3 : 0;
            }
            case Literal -> {
                if (args[offset].equals(literalValue)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }

        throw new IllegalStateException("Shouldn't be possible to be here");
    }

    @Override
    public String toString() {
        return switch (type) {
            case Literal -> "Literal(" + literalValue + ")";
            default -> type.toString();
        };
    }
}
