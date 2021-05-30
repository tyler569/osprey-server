package io.philbrick.minecraft;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    enum ParameterType {
        Boolean,
        Float,
        Integer,
        String,
        Entity,
        Player,
        Vec2,
        Vec3,
        NBT,
        Literal;

        static boolean matches(ParameterType[] parameterTypes, String[] args) {
            return false;
        }

        String brigadierType() {
            return switch (this) {
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
    }

    String name() default "";
    ParameterType[] args() default {};
}