package com.pygostylia.osprey;

import com.pygostylia.osprey.commands.CommandBucket;
import com.pygostylia.osprey.entities.Entity;
import com.pygostylia.osprey.entities.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Main {
    // TODO: read these in from configuration
    public static final String brand = "Osprey";
    public static ChunkDispatcher chunkDispatcher = new ChunkDispatcher();
    public static KeyPair encryptionKey;
    public static World world;
    public static CommandBucket commands;
    static int nextEntityId = 1;
    public static Scheduler scheduler = new Scheduler();

    public static String handshakeJson() {
        var result = new JSONObject();
        var version = new JSONObject();
        var players = new JSONObject();
        var description = new JSONObject();
        var playerSample = new JSONArray();

        version.put("name", "1.16.5");
        version.put("protocol", 754);

        Player.forEach(player -> {
            var playerJson = new JSONObject();
            playerJson.put("name", player.name());
            playerJson.put("id", player.uuid());
            playerSample.put(playerJson);
        });

        players.put("max", 10);
        players.put("online", Player.count());
        players.put("sample", playerSample);

        description.put("text", "Hello World!");

        result.put("version", version);
        result.put("players", players);
        result.put("description", description);

        return result.toString();
    }

    public static void main(String[] args) throws IOException, SQLException, NoSuchAlgorithmException {
        world = World.open("world.db");

        final var kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);
        encryptionKey = kpg.generateKeyPair();

        Registry.setup("generated");
        BlockState.setup();
        commands = new CommandBucket();
        commands.register(Commands.class);

        new Thread(chunkDispatcher).start();
        new Thread(scheduler).start();

        final var socket = new ServerSocket(25565);
        System.out.println("Ready");
        while (!socket.isClosed()) {
            var connection = socket.accept();
            Player.runThread(connection);
        }
    }
}
