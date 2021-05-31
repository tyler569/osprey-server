package io.philbrick.minecraft;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandAliases {
    CommandAlias[] value();
}
