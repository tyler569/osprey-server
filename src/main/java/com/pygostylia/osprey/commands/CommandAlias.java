package com.pygostylia.osprey.commands;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(CommandAliases.class)
public @interface CommandAlias {
    String value();
}
