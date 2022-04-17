package com.pygostylia.osprey.commands3

abstract class Command {
    abstract val name: String
}

class Teleport : Command() {
    override val name: String = "teleport"
}
