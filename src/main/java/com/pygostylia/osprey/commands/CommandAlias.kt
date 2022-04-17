package com.pygostylia.osprey.commands

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@JvmRepeatable(CommandAliases::class)
annotation class CommandAlias(val value: String)