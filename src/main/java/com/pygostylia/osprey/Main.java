package com.pygostylia.osprey;

import com.pygostylia.osprey.nbt.NBTCompound;
import com.pygostylia.osprey.nbt.NBTList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class Main {
    // TODO: read these in from configuration
    static NBTCompound overworldDimension = new NBTCompound();
    static {
        overworldDimension.put("piglin_safe", (byte) 0);
        overworldDimension.put("natural", (byte) 1);
        overworldDimension.put("ambient_light", 1.0f);
        overworldDimension.put("infiniburn", "minecraft:infiniburn_overworld");
        overworldDimension.put("respawn_anchor_works", (byte) 0);
        overworldDimension.put("has_skylight", (byte) 1);
        overworldDimension.put("bed_works", (byte) 1);
        overworldDimension.put("effects", "minecraft:overworld");
        overworldDimension.put("has_raids", (byte) 1);
        overworldDimension.put("logical_height", 256);
        overworldDimension.put("coordinate_scale", 1.0);
        overworldDimension.put("ultrawarm", (byte) 0);
        overworldDimension.put("has_ceiling", (byte) 0);
    }

    static NBTCompound dimensionCodec = new NBTCompound(null);
    static {
        var dimensionType = new NBTCompound();
        dimensionType.put("type", "minecraft:dimension_type");

        var dimensionValues = new NBTList<NBTCompound>();

        var overworld = new NBTCompound();
        overworld.put("name", "minecraft:overworld");
        overworld.put("id", 0);
        overworld.put("element", overworldDimension);

        dimensionValues.add(overworld);
        dimensionType.put("value", dimensionValues);
        dimensionCodec.put("minecraft:dimension_type", dimensionType);

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

        dimensionCodec.put("minecraft:worldgen/biome", biomes);
    }

    static final String brand = "Osprey";
    private static final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private static final Map<Integer, Entity> entities = new ConcurrentHashMap<>();
    static ChunkDispatcher chunkDispatcher = new ChunkDispatcher();
    static Registry registry;
    static KeyPair encryptionKey;
    static World world;
    static CommandBucket commands;
    static byte[] commandPacket;
    static int nextEntityId = 1;
    static EntityController entityController = new EntityController();

    static void addPlayer(Player player) {
        players.put(player.id, player);
    }

    static void removePlayer(Player player) {
        players.remove(player.id);
    }

    static void addEntity(Entity entity) {
        entities.put(entity.id, entity);
    }

    static void removeEntity(Entity entity) {
        entities.remove(entity.id);
    }

    static Collection<Player> allPlayers() {
        return players.values();
    }

    static Player playerByName(String name) {
        return players.values().stream()
                .filter((player) -> player.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    static Player playerByEntityId(int entityId) {
        return players.get(entityId);
    }

    static Optional<Entity> entityById(int entityId) {
        return Optional.ofNullable(entities.get(entityId));
    }

    static void forEachPlayer(PlayerIOLambda lambda) throws IOException {
        for (var player : Main.players.values()) {
            lambda.apply(player);
        }
    }

    static Stream<Player> playersWithin(int radius, Location location) {
        return players.values().stream().filter((player) -> player.location().withinRadiusOf(radius, location));
    }

    static String handshakeJson() throws IOException {
        var result = new JSONObject();
        var version = new JSONObject();
        var players = new JSONObject();
        var description = new JSONObject();
        var playerSample = new JSONArray();

        version.put("name", "1.16.5");
        version.put("protocol", 754);

        forEachPlayer((player) -> {
            var playerJson = new JSONObject();
            playerJson.put("name", player.name);
            playerJson.put("id", player.uuid);
            playerSample.put(playerJson);
        });

        players.put("max", 10);
        players.put("online", Main.players.size());
        players.put("sample", playerSample);

        description.put("text", "Hello World!");

        result.put("version", version);
        result.put("players", players);
        result.put("description", description);

        return result.toString();
    }

    public static void main(String[] args) throws IOException, SQLException, NoSuchAlgorithmException {
        registry = new Registry("generated");

        world = World.open("world.db");

        final var kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        encryptionKey = kpg.generateKeyPair();

        commands = new CommandBucket();
        commandPacket = commands.brigadierPacket();

        new Thread(chunkDispatcher).start();
        new Thread(entityController).start();

        final var socket = new ServerSocket(25565);
        System.out.println("Ready");
        while (!socket.isClosed()) {
            var connection = socket.accept();
            Player.runThread(connection);
        }
    }
}
