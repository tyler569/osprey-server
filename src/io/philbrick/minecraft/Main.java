package io.philbrick.minecraft;

import io.philbrick.minecraft.nbt.*;
import org.json.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.util.*;

public class Main {
    static final String handshake_json = """
        {
          "version": {
            "name": "1.16.5",
            "protocol": 754
          },
          "players": {
            "max": 10,
            "online": 1,
            "sample": [
              {
                "name": "tyler569",
                "id": "492c1496-f590-4a6f-a899-37f9b87a8238"
              }
            ]
          },
          "description": {
            "text": "Hello World!"
          }
        }
        """;

    static final NBTValue dimension = new NBTCompound(null,
        new NBTByte("piglin_safe", 0),
        new NBTByte("natural", 1),
        new NBTFloat("ambient_light", 1.0f),
        new NBTString("infiniburn", "minecraft:infiniburn_overworld"),
        new NBTByte("respawn_anchor_works", 0),
        new NBTByte("has_skylight", 1),
        new NBTByte("bed_works", 1),
        new NBTString("effects", "minecraft:overworld"),
        new NBTByte("has_raids", 1),
        new NBTInteger("logical_height", 256),
        new NBTDouble("coordinate_scale", 1.0),
        new NBTByte("ultrawarm", 0),
        new NBTByte("has_ceiling", 0)
    );

    static final NBTValue dimensionCodec = new NBTCompound(null,
        new NBTCompound("minecraft:dimension_type",
            new NBTString("type", "minecraft:dimension_type"),
            new NBTList<>("value",
                new NBTCompound(null,
                    new NBTString("name", "minecraft:overworld"),
                    new NBTInteger("id", 0),
                    new NBTCompound("element",
                        new NBTByte("piglin_safe", 0),
                        new NBTByte("natural", 1),
                        new NBTFloat("ambient_light", 1.0f),
                        new NBTString("infiniburn", "minecraft:infiniburn_overworld"),
                        new NBTByte("respawn_anchor_works", 0),
                        new NBTByte("has_skylight", 1),
                        new NBTByte("bed_works", 1),
                        new NBTString("effects", "minecraft:overworld"),
                        new NBTByte("has_raids", 1),
                        new NBTInteger("logical_height", 256),
                        new NBTDouble("coordinate_scale", 1.0),
                        new NBTByte("ultrawarm", 0),
                        new NBTByte("has_ceiling", 0)
                    )
                )
            )
        ),
        new NBTCompound("minecraft:worldgen/biome",
            new NBTString("type", "minecraft:worldgen/biome"),
            new NBTList<>("value",
                new NBTCompound(null,
                    new NBTString("name", "minecraft:plains"),
                    new NBTInteger("id", 0),
                    new NBTCompound("element",
                        new NBTString("precipitation", "rain"),
                        new NBTCompound("effects",
                            new NBTInteger("sky_color", 7907327),
                            new NBTInteger("water_fog_color", 329011),
                            new NBTInteger("fog_color", 12638463),
                            new NBTInteger("water_color", 4159204),
                            new NBTCompound("mood_sound",
                                new NBTInteger("tick_delay", 6000),
                                new NBTDouble("offset", 2.0),
                                new NBTString("sound", "minecraft:ambient_cave"),
                                new NBTInteger("block_search_extent", 0)
                            )
                        ),
                        new NBTFloat("depth", 0.125f),
                        new NBTFloat("temperature", 0.8f),
                        new NBTFloat("scale", 0.05f),
                        new NBTFloat("downfall", 0.4f),
                        new NBTString("category", "plains")
                    )
                )
            )
        )
    );

    static KeyPair encryptionKey;
    static Set<Player> players = new HashSet<>();
    static Map<String, Position> playerLocations = new HashMap<>();
    static World world;

    static int nextEntityId = 1;
    static boolean preload = false;

    static Map<Integer, Integer> itemToBlock = new HashMap<>();

    static void populateItemToBlock() throws IOException {
        var registryFile = Files.readString(Path.of("generated/reports/registries.json"));
        var registry = new JSONObject(registryFile);
        var items = registry.getJSONObject("minecraft:item").getJSONObject("entries");
        Iterator<String> itemsKeys = items.keys();

        var blockRegistry = Files.readString(Path.of("generated/reports/blocks.json"));
        var blocks = new JSONObject(blockRegistry);

        while (itemsKeys.hasNext()) {
            String key = itemsKeys.next();
            var itemId = items.getJSONObject(key).getInt("protocol_id");
            var blockEntry = blocks.optJSONObject(key);
            if (blockEntry != null) {
                var blockId = blockEntry.getJSONArray("states").getJSONObject(0).getInt("id");
                itemToBlock.put(itemId, blockId);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            world = World.open("world.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        populateItemToBlock();

        if (preload) {
            try {
                world.preloadFromDisk(
                    new ChunkLocation(-20, -20),
                    new ChunkLocation(20, 20)
                );
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        try {
            var kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            encryptionKey = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        final var socket = new ServerSocket(25565);
        System.out.println("Ready");
        while (!socket.isClosed()) {
            var connection = socket.accept();
            Player.runThread(nextEntityId, connection);
            nextEntityId += 1;
        }
    }
}
