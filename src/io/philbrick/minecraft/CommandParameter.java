package io.philbrick.minecraft;

import java.util.regex.*;

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

        if (type == null) {
            pt.literalValue = name;
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

    public boolean matches(CommandParameter other) {
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

    @Override
    public String toString() {
        return switch (type) {
            case Literal -> "Literal(" + literalValue + ")";
            default -> type.toString();
        };
    }
}
