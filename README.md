# Osprey
Osprey is a Minecraft server written in Java that contains no code from Mojang

## Building
Osprey uses the Maven build system. You should be able to import this project into your IDE or run `mvn package`

## Running
To run Osprey, you will need the minecraft generated JSON registries; use the procedure [here](https://wiki.vg/Data_Generators) to generate the `--reports` generation, and put the generated `generated` folder next to `src` in this project directory.
Osprey will generate a new world file on first run, this is currently hardcoded to `world.db` in the project folder, and will read that file when it is run subsequent times. You can back that world up, all player, block, and entity information is saved in the one file.

I intend to keep the world format compatible across Osprey revisions and Minecraft upgrades.

## Features
Today, Osprey supports
* Protocol encryption and compression
* Multiple players
* Placing and destroying almost all blocks
* Infinite flat world generation
* World persistence with a custom world format
* Command definition and autocompletion generation
* Basic Boat and Arrow mechanics
* Explosive Arrows!
