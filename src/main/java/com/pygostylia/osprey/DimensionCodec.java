package com.pygostylia.osprey;

import com.pygostylia.osprey.nbt.NBTCompound;
import com.pygostylia.osprey.nbt.NBTList;

public class DimensionCodec {
    public static NBTCompound overworld = new NBTCompound();
    public static NBTCompound codec = new NBTCompound(null);

    static {
        overworld.put("piglin_safe", (byte) 0);
        overworld.put("natural", (byte) 1);
        overworld.put("ambient_light", 1.0f);
        overworld.put("infiniburn", "minecraft:infiniburn_overworld");
        overworld.put("respawn_anchor_works", (byte) 0);
        overworld.put("has_skylight", (byte) 1);
        overworld.put("bed_works", (byte) 1);
        overworld.put("effects", "minecraft:overworld");
        overworld.put("has_raids", (byte) 1);
        overworld.put("logical_height", 256);
        overworld.put("coordinate_scale", 1.0);
        overworld.put("ultrawarm", (byte) 0);
        overworld.put("has_ceiling", (byte) 0);
    }

    static {
        var dimensionType = new NBTCompound();
        dimensionType.put("type", "minecraft:dimension_type");

        var dimensionValues = new NBTList<NBTCompound>();

        var overworld = new NBTCompound();
        overworld.put("name", "minecraft:overworld");
        overworld.put("id", 0);
        overworld.put("element", DimensionCodec.overworld);

        dimensionValues.add(overworld);
        dimensionType.put("value", dimensionValues);
        codec.put("minecraft:dimension_type", dimensionType);

        var biomes = new NBTCompound();
        biomes.put("type", "minecraft:worldgen/biome");

        var biomeValues = new NBTList<NBTCompound>();

        var plains = new NBTCompound();
        plains.put("name", "minecraft:plains");
        plains.put("id", 0);

        var plainsElement = new NBTCompound();
        plainsElement.put("precipitation", "rain");
        plainsElement.put("depth", 0.125f);
        plainsElement.put("temperature", 0.8f);
        plainsElement.put("scale", 0.05f);
        plainsElement.put("downfall", 0.4f);
        plainsElement.put("category", "plains");

        var plainsEffects = new NBTCompound();
        plainsEffects.put("sky_color", 7907327);
        plainsEffects.put("water_fog_color", 329011);
        plainsEffects.put("fog_color", 12638463);
        plainsEffects.put("water_color", 4159204);

        var plainsMoodSounds = new NBTCompound();
        plainsMoodSounds.put("tick_delay", 6000);
        plainsMoodSounds.put("offset", 2.0);
        plainsMoodSounds.put("sound", "minecraft:ambient_cave");
        plainsMoodSounds.put("block_search_extent", 0);

        plainsEffects.put("mood", plainsMoodSounds);
        plainsElement.put("effects", plainsEffects);
        plains.put("element", plainsElement);
        biomeValues.add(plains);
        biomes.put("value", biomeValues);

        codec.put("minecraft:worldgen/biome", biomes);
    }
}
