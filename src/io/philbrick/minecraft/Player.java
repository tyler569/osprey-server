package io.philbrick.minecraft;

import org.json.*;

import javax.crypto.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.time.*;
import java.util.*;

public class Player {
    enum State {
        Status,
        Login,
        Play,
    }

    static final byte[] encryptionToken = "Hello World".getBytes();
    static final int maxRenderDistance = 10;

    String name;
    Connection connection;
    Duration ping;
    Thread thread;
    State state;
    int entityId;
    UUID uuid;
    ArrayList<Player> nearbyPlayers;
    Position position;
    Inventory inventory;
    Set<Location> loadedChunks;
    int renderDistance;
    boolean firstSettings = true;

    Chunk theChunk;

    boolean printUnknownPackets = true;

    Player(int entity, Socket sock) throws IOException {
        connection = new Connection(sock);
        state = State.Status;
        entityId = entity;
        thread = new Thread(this::connectionWrapper);
        thread.start();
    }

    static void runThread(int entityId, Socket sock) throws IOException {
        new Player(entityId, sock);
    }

    void connectionWrapper() {
        try {
            handleConnection();
        } catch (EOFException e) {
            disconnect();
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }

    void disconnect() {
        if (connection.isEstablished()) {
            for (var player : Main.players) {
                try {
                    player.sendDespawnPlayer(this);
                    player.sendRemovePlayer(this);
                    player.sendNotification(String.format("%s has left the game.", name));
                } catch (IOException ignored) {
                }
            }
            Main.playerLocations.put(name, position);
        }
        Main.reaper.appendDeadThread(thread);
        Main.players.remove(this);
    }

    void teleport(Location location) throws IOException {
        position = new Position(location);
        sendPositionLook();
        sendUpdateChunkPosition();
        for (var player : Main.players) {
            if (player == this) continue;
            player.sendEntityTeleport(entityId, position);
        }
        loadCorrectChunks();
    }

    void handleConnection() throws IOException {
        while (!connection.isClosed()) {
            Packet p = connection.readPacket();
            handlePacket(p);
        }
        System.out.println("Leaving handleConnection");
    }

    void handlePacket(Packet packet) throws IOException {
        switch (state) {
            case Status -> handleStatusPacket(packet);
            case Login -> handleLoginPacket(packet);
            case Play -> handlePlayPacket(packet);
        }
    }

    void unknownPacket(Packet packet) {
        System.out.format("Unknown packet type %d %#x in state %s%n", packet.type, packet.type, state);
    }

    void handleStatusPacket(Packet packet) throws IOException {
        switch (packet.type) {
            case 0 -> handleHandshake(packet);
            case 1 -> handlePing(packet);
            default -> unknownPacket(packet);
        }
    }

    void handleLoginPacket(Packet packet) throws IOException {
        switch (packet.type) {
            case 0 -> handleLoginStart(packet);
            case 1 -> handleEncryptionResponse(packet);
            default -> unknownPacket(packet);
        }
    }

    void handlePlayPacket(Packet packet) throws IOException {
        switch (packet.type) {
            case 3 -> handleChat(packet);
            case 5 -> handleSettings(packet);
            case 16 -> handleKeepAlive(packet);
            case 18 -> handlePosition(packet);
            case 19 -> handlePositionAndRotation(packet);
            case 20 -> handleRotation(packet);
            case 21 -> handleMovement(packet);
            case 27 -> handlePlayerDigging(packet);
            case 28 -> handleEntityAction(packet);
            case 44 -> handleAnimation(packet);
            case 46 -> handlePlayerPlaceBlock(packet);
            default -> unknownPacket(packet);
        }
    }


    void writeHandshakeResponse() throws IOException {
        connection.sendPacket(0, (m) -> {
            VarInt.write(m, Main.handshake_json.length());
            m.write(Main.handshake_json.getBytes());
        });
    }

    void writePingResponse(long number) throws IOException {
        connection.sendPacket(1, (m) -> {
            var b = ByteBuffer.allocate(Long.BYTES);
            b.order(ByteOrder.BIG_ENDIAN);
            b.putLong(number);
            m.write(b.array());
        });
    }

    // handle Status packets

    void handleHandshake(Packet packet) throws IOException {
        if (packet.originalLen > 2) {
            int protocolVersion = packet.readVarInt();
            String address = packet.readString();
            short port = packet.readShort();
            int next = packet.readVarInt();

            System.out.format("handshake: version: %d address: '%s' port: %d next: %d%n",
                protocolVersion, address, port, next);
            if (next == 2) {
                state = State.Login;
            }
        } else {
            writeHandshakeResponse();
        }
    }

    void handlePing(Packet packet) throws IOException {
        long number = packet.readLong();
        writePingResponse(number);
    }

    // handle login packets

    void sendEncryptionRequest() throws IOException {
        connection.sendPacket(1, (m) -> { // encryption request
            Protocol.writeString(m, "server id not short");
            var encodedKey = Main.encryptionKey.getPublic().getEncoded();
            Protocol.writeVarInt(m, encodedKey.length);
            Protocol.writeBytes(m, encodedKey);
            Protocol.writeVarInt(m, encryptionToken.length);
            Protocol.writeBytes(m, encryptionToken);
        });
    }

    void handleLoginStart(Packet packet) throws IOException {
        name = packet.readString();
        uuid = UUID.nameUUIDFromBytes(String.format("OfflinePlayer:%s", name).getBytes());
        // uuid = UUID.randomUUID();
        System.out.format("login: '%s'%n", name);
        sendEncryptionRequest();
    }

    void sendLoginSuccess() throws IOException {
        connection.sendPacket(2, (m) -> { // login success
            Protocol.writeLong(m, 0);
            Protocol.writeLong(m, 0);
            Protocol.writeString(m, name);
        });
    }

    void sendBrand(String brand) throws IOException {
        connection.sendPacket(0x17, (m) -> {
            Protocol.writeString(m, "minecraft:brand");
            Protocol.writeBytes(m, brand.getBytes());
        });
    }

    void sendJoinGame() throws IOException {
        connection.sendPacket(0x24, (m) -> { // Join Game
            Protocol.writeInt(m, entityId);
            Protocol.writeBoolean(m, false); // is hardcore
            Protocol.writeByte(m, (byte)1); // gamemode creative
            Protocol.writeByte(m, (byte)-1); // previous gamemode
            Protocol.writeVarInt(m, 1); // world count
            Protocol.writeString(m, "minecraft:overworld"); // list of worlds (# count)
            Main.dimensionCodec.encode(m);
            Main.dimension.encode(m);
            Protocol.writeString(m, "minecraft:overworld"); // world name
            Protocol.writeLong(m, 1); // hashed seed
            Protocol.writeVarInt(m, 100); // max players
            Protocol.writeVarInt(m, 10); // view distance
            Protocol.writeBoolean(m, false); // reduce debug info
            Protocol.writeBoolean(m, true); // enable respawn screen
            Protocol.writeBoolean(m, false); // world is debug (never)
            Protocol.writeBoolean(m, true); // world is superflat
        });
    }

    void join() throws IOException {
        sendJoinGame();
        sendAddPlayers(Main.players);
        sendAddPlayer(this);
        sendBrand("BeforeBrand");
        position = Main.playerLocations.getOrDefault(name, new Position());
        sendUpdateChunkPosition();
        inventory = new Inventory();
        loadedChunks = new HashSet<>();
        theChunk = Main.theChunk;
        sendInventory();
        for (var player : Main.players) {
            player.sendAddPlayer(this);
            player.sendSpawnPlayer(this);
            sendSpawnPlayer(player);
        }
        Main.players.add(this);
        for (var player : Main.players) {
            player.sendNotification(String.format("%s has joined the game (id %d)", name, entityId));
        }
    }

    void sendEntityTeleport(int entityId, Position position) throws IOException {
        connection.sendPacket(0x56, (m) -> {
            Protocol.writeVarInt(m, entityId);
            Protocol.writeDouble(m, position.x);
            Protocol.writeDouble(m, position.y);
            Protocol.writeDouble(m, position.z);
            Protocol.writeByte(m, position.yawAngle());
            Protocol.writeByte(m, position.pitchAngle());
            Protocol.writeBoolean(m, position.onGround);
        });
    }

    // TODO: break this up
    void handleEncryptionResponse(Packet packet) throws IOException {
        int secretLength = packet.readVarInt();
        byte[] secret = packet.readNBytes(secretLength);
        int tokenLength = packet.readVarInt();
        byte[] token = packet.readNBytes(tokenLength);

        byte[] decryptedSecret;
        byte[] decryptedToken;

        try {
            var cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, Main.encryptionKey.getPrivate());
            decryptedToken = cipher.doFinal(token);
            System.out.format("  decrypted token: %s%n",
                Arrays.toString(decryptedToken));

            cipher.init(Cipher.DECRYPT_MODE, Main.encryptionKey.getPrivate());
            decryptedSecret = cipher.doFinal(secret);
            System.out.format("  decrypted secret key: %s%n",
                Arrays.toString(decryptedSecret));
        } catch (Exception e) {
            e.printStackTrace();
            // login failure
            return;
        }

        try {
            connection.setEncryption(decryptedSecret);
        } catch (Exception e) {
            // send Login Failure
            System.out.println("Encryption failure!");
            return;
        }

        // setCompression(1024);
        System.out.println("Writing Login Success!");
        sendLoginSuccess();
        state = State.Play;
        join();
    }

    // handle play packets

    void sendChunk(int x, int z) throws IOException {
        connection.sendPacket(0x20, (m) -> {
            Protocol.writeInt(m, x);
            Protocol.writeInt(m, z);
            theChunk.encodePacket(m);
        });
        loadedChunks.add(new Location(x, 0, z));
    }

    void unloadChunk(int x, int z) throws IOException {
        connection.sendPacket(0x1c, (m) -> {
            Protocol.writeInt(m, x);
            Protocol.writeInt(m, z);
        });
        loadedChunks.remove(new Location(x, 0, z));
    }

    void unloadChunkSafe(int x, int z) {
        try {
            unloadChunk(x, z);
        } catch (IOException ignored) {}
    }

    void sendInventory() throws IOException {
        connection.sendPacket(0x13, (m) -> {
            Protocol.writeByte(m, 0);
            Protocol.writeShort(m, inventory.size());
            for (var slot : inventory.slots) {
                slot.encode(m);
            }
        });
    }

    void sendPositionLook() throws IOException {
        connection.sendPacket(0x34, (m) -> {
            Protocol.writeDouble(m, position.x);
            Protocol.writeDouble(m, position.y);
            Protocol.writeDouble(m, position.z);
            Protocol.writeFloat(m, position.yaw);
            Protocol.writeFloat(m, position.pitch);
            Protocol.writeByte(m, 0);
            Protocol.writeVarInt(m, 0);
        });
    }

    void sendUpdateChunkPosition(int x, int z) throws IOException {
        connection.sendPacket(0x40, (m) -> {
            Protocol.writeVarInt(m, x);
            Protocol.writeVarInt(m, z);
        });
    }

    void sendUpdateChunkPosition() throws IOException {
        sendUpdateChunkPosition(position.chunkX(), position.chunkZ());
    }

    void sendSpawnPosition() throws IOException {
        connection.sendPacket(0x42, (m) -> {
            Protocol.writePosition(m, 0, 32, 0);
        });
    }

    void initialSpawnPlayer() throws IOException {
        sendSpawnPosition();
        loadCorrectChunks();
        sendUpdateChunkPosition();
        sendPositionLook();
        sendBrand("AfterBrand");
    }

    void handleSettings(Packet packet) throws IOException {
        String locale = packet.readString();
        renderDistance = packet.read();
        if (renderDistance > maxRenderDistance) renderDistance = maxRenderDistance;
        int chatMode = packet.readVarInt();
        boolean chatColors = packet.readBoolean();
        int skinParts = packet.read();
        int mainHand = packet.readVarInt();
        if (firstSettings) {
            firstSettings = false;
            initialSpawnPlayer();
        } else {
            loadCorrectChunks();
        }
    }

    void handleKeepAlive(Packet packet) throws IOException {
        long keepAlive = packet.readLong();
        if (!connection.validateKeepAlive(keepAlive)) {
            System.out.println("Keepalives did not match!");
        }
        ping = connection.pingTime();
    }

    void sendAddPlayers(Collection<Player> players) throws IOException {
        if (players.size() == 0) {
            return;
        }
        connection.sendPacket(0x32, (m) -> {
            Protocol.writeVarInt(m, 0);
            Protocol.writeVarInt(m, players.size());
            for (var player : players) {
                Protocol.writeLong(m, player.uuid.getMostSignificantBits());
                Protocol.writeLong(m, player.uuid.getLeastSignificantBits());
                Protocol.writeString(m, player.name);
                Protocol.writeVarInt(m, 0); // number of properties
                Protocol.writeVarInt(m, 1); // gamemode
                Protocol.writeVarInt(m, 0); // ping
                Protocol.writeBoolean(m, false); // has display name
            }
        });
    }

    void sendAddPlayer(Player player) throws IOException {
        connection.sendPacket(0x32, (m) -> {
            Protocol.writeVarInt(m, 0);
            Protocol.writeVarInt(m, 1);
            Protocol.writeLong(m, player.uuid.getMostSignificantBits());
            Protocol.writeLong(m, player.uuid.getLeastSignificantBits());
            Protocol.writeString(m, player.name);
            Protocol.writeVarInt(m, 0); // number of properties
            Protocol.writeVarInt(m, 1); // gamemode
            Protocol.writeVarInt(m, 0); // ping
            Protocol.writeBoolean(m, false); // has display name
        });
    }

    void sendSpawnPlayer(Player player) throws IOException {
        connection.sendPacket(4, (m) -> {
            Protocol.writeVarInt(m, player.entityId);
            Protocol.writeLong(m, player.uuid.getMostSignificantBits());
            Protocol.writeLong(m, player.uuid.getLeastSignificantBits());
            Protocol.writeDouble(m, player.position.x);
            Protocol.writeDouble(m, player.position.y);
            Protocol.writeDouble(m, player.position.z);
            Protocol.writeByte(m, player.position.yawAngle());
            Protocol.writeByte(m, player.position.pitchAngle());
        });
    }

    void sendRemovePlayer(Player player) throws IOException {
        connection.sendPacket(0x32, (m) -> {
            Protocol.writeVarInt(m, 4);
            Protocol.writeVarInt(m, 1);
            Protocol.writeLong(m, player.uuid.getMostSignificantBits());
            Protocol.writeLong(m, player.uuid.getLeastSignificantBits());
        });
    }

    void sendDespawnPlayer(Player player) throws IOException {
        connection.sendPacket(0x36, (m) -> {
            Protocol.writeVarInt(m, 1);
            Protocol.writeVarInt(m, player.entityId);
        });
    }

    void sendChat(Player sender, String message) throws IOException {
        var chat = new JSONObject();
        chat.put("text", String.format("<%s> %s", sender.name, message));
        connection.sendPacket(0x0E, (m) -> {
            Protocol.writeString(m, chat.toString());
            Protocol.writeByte(m, 0);
            Protocol.writeLong(m, sender.uuid.getMostSignificantBits());
            Protocol.writeLong(m, sender.uuid.getLeastSignificantBits());
        });
    }

    void sendSystemMessage(String message, String color) throws IOException {
        var chat = new JSONObject();
        chat.put("text", message);
        chat.put("color", color);
        connection.sendPacket(0x0E, (m) -> {
            Protocol.writeString(m, chat.toString());
            Protocol.writeByte(m, 0);
            Protocol.writeLong(m, 0);
            Protocol.writeLong(m, 0);
        });
    }

    void sendNotification(String message) throws IOException {
        sendSystemMessage(message, "yellow");
    }

    void sendError(String message) throws IOException {
        sendSystemMessage(message, "red");
    }

    void handleChat(Packet packet) throws IOException {
        String message = packet.readString();
        System.out.format("[chat] %s: %s%n", name, message);
        if (message.startsWith("/")) {
            var parts = message.split(" ");
            switch (parts[0]) {
                case "/tp" -> {
                    try {
                        teleport(new Location(
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3]))
                        );
                    } catch (NumberFormatException e) {
                        sendError(String.format("Invalid teleport command \"%s\"", message));
                    }
                }
                default -> sendError(String.format("Invalid command \"%s\"", parts[0]));
            }
            return;
        }
        for (var player : Main.players) {
            player.sendChat(this, message);
        }
    }

    // movement

    void sendEntityPositionAndRotation(int entityId, double dx, double dy, double dz, Position position) throws IOException {
        short protocol_dx = (short)(dx * 4096);
        short protocol_dy = (short)(dy * 4096);
        short protocol_dz = (short)(dz * 4096);
        connection.sendPacket(0x28, (m) -> {
            Protocol.writeVarInt(m, entityId);
            Protocol.writeShort(m, protocol_dx);
            Protocol.writeShort(m, protocol_dy);
            Protocol.writeShort(m, protocol_dz);
            Protocol.writeByte(m, position.yawAngle());
            Protocol.writeByte(m, position.pitchAngle());
            Protocol.writeBoolean(m, position.onGround);
        });
        connection.sendPacket(0x3A, (m) -> {
            Protocol.writeVarInt(m, entityId);
            Protocol.writeByte(m, position.yawAngle());
        });
    }

    void loadCorrectChunks(int chunkX, int chunkZ) throws IOException {
        Set<Location> shouldLoad = new HashSet<>();
        for (int cx = chunkX - renderDistance; cx <= chunkX + renderDistance; cx++) {
            for (int cz = chunkZ - renderDistance; cz <= chunkZ + renderDistance; cz++) {
                shouldLoad.add(new Location(cx, 0, cz));
            }
        }
        var unloadChunks = new HashSet<>(loadedChunks);
        unloadChunks.removeAll(shouldLoad);
        for (var chunk : unloadChunks) {
            unloadChunk(chunk.x(), chunk.z());
            // System.out.printf("unload %d %d%n", chunk.x(), chunk.z());
        }
        shouldLoad.removeAll(loadedChunks);
        for (var chunk : shouldLoad) {
            sendChunk(chunk.x(), chunk.z());
            // System.out.printf("load %d %d%n", chunk.x(), chunk.z());
        }
    }

    void loadCorrectChunks() throws IOException {
        loadCorrectChunks(position.chunkX(), position.chunkZ());
    }

    void checkChunkPosition(double x, double z) throws IOException {
        int chunkX = (int)x >> 4;
        int chunkZ = (int)z >> 4;
        if (chunkX != position.chunkX() || chunkZ != position.chunkZ()) {
            sendUpdateChunkPosition(chunkX, chunkZ);
            System.out.format("new chunk %d %d%n", chunkX, chunkZ);
            loadCorrectChunks(chunkX, chunkZ);
        }
    }

    void updatePosition(double x, double y, double z, float pitch, float yaw, boolean onGround) throws IOException {
        double delta_x = x - position.x;
        double delta_y = y - position.y;
        double delta_z = z - position.z;
        checkChunkPosition(x, z);
        position.x = x;
        position.y = y;
        position.z = z;
        position.pitch = pitch;
        position.yaw = yaw;
        position.onGround = onGround;
        for (var player : Main.players) {
            if (player == this) {
                continue;
            }
            player.sendEntityPositionAndRotation(entityId, delta_x, delta_y, delta_z, position);
        }
    }

    void updatePosition(double x, double y, double z, boolean onGround) throws IOException {
        updatePosition(x, y, z, position.pitch, position.yaw, onGround);
    }

    void updatePosition(float pitch, float yaw, boolean onGround) throws IOException {
        updatePosition(position.x, position.y, position.z, pitch, yaw, onGround);
    }

    void updatePosition(boolean onGround) throws IOException {
        updatePosition(position.x, position.y, position.z, position.pitch, position.yaw, onGround);
    }

    void handlePosition(Packet packet) throws IOException {
        double x = packet.readDouble();
        double y = packet.readDouble();
        double z = packet.readDouble();
        boolean onGround = packet.readBoolean();
        updatePosition(x, y, z, onGround);
    }

    void handlePositionAndRotation(Packet packet) throws IOException {
        double x = packet.readDouble();
        double y = packet.readDouble();
        double z = packet.readDouble();
        float yaw = packet.readFloat();
        float pitch = packet.readFloat();
        boolean onGround = packet.readBoolean();
        updatePosition(x, y, z, pitch, yaw, onGround);
    }

    void handleRotation(Packet packet) throws IOException {
        float yaw = packet.readFloat();
        float pitch = packet.readFloat();
        boolean onGround = packet.readBoolean();
        updatePosition(pitch, yaw, onGround);
    }

    void handleMovement(Packet packet) throws IOException {
        boolean onGround = packet.readBoolean();
        updatePosition(onGround);
    }

    boolean intersectsLocation(Location location) {
        if (position.x + 0.3 < location.x() || location.x() + 1 < position.x - 0.3) return false;
        if (position.z + 0.3 < location.z() || location.z() + 1 < position.z - 0.3) return false;
        if (position.y + 1.8 < location.y() || location.y() + 1 <= position.y) return false;

        return true;
    }

    // animation

    void sendEntityAnimation(int entityId, int animation) throws IOException {
        connection.sendPacket(5, (m) -> {
            Protocol.writeVarInt(m, entityId);
            Protocol.writeByte(m, animation);
        });
    }

    void handleEntityAction(Packet packet) {}

    void handleAnimation(Packet packet) throws IOException {
        for (var player : Main.players) {
            if (player == this) continue;
            player.sendEntityAnimation(this.entityId, 0);
        }
    }

    // block place & remove

    void sendBlockChange(Location location, int blockId) throws IOException {
        connection.sendPacket(0xB, (m) -> {
            Protocol.writeLong(m, location.encode());
            Protocol.writeVarInt(m, blockId);
        });
    }

    void handlePlayerDigging(Packet packet) throws IOException {
        var status = packet.readVarInt();
        var location = packet.readLocation();
        var face = packet.read();
        System.out.format("%s Dig %s : %d%n", name, location, status);
        theChunk.setBlock(location.positionInChunk(), 0);
        for (var player : Main.players) {
            if (player == this) continue;
            player.sendBlockChange(location, 0);
        }
    }

    void handlePlayerPlaceBlock(Packet packet) throws IOException {
        var hand = packet.readVarInt();
        var location = packet.readLocation();
        var face = packet.readVarInt();
        switch (face) {
            case 0 -> location = location.offset(0, -1, 0);
            case 1 -> location = location.offset(0, 1, 0);
            case 2 -> location = location.offset(0, 0, -1);
            case 3 -> location = location.offset(0, 0, 1);
            case 4 -> location = location.offset(-1, 0, 0);
            case 5 -> location = location.offset(1, 0, 0);
        }
        float cursorX = packet.readFloat();
        float cursorY = packet.readFloat();
        float cursorZ = packet.readFloat();
        boolean insideBlock = packet.readBoolean();
        System.out.printf("Place %d %s %s %f %f %f %b%n", hand, location, face, cursorX, cursorY, cursorZ, insideBlock);
        for (var player : Main.players) {
            if (player.intersectsLocation(location)) {
                sendBlockChange(location, 0);
                return;
            }
        }
        theChunk.setBlock(location.positionInChunk(), 1);
        for (var player : Main.players) {
            player.sendBlockChange(location, 1);
        }
    }

}