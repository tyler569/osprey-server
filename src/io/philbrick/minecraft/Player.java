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
    static final int maxRenderDistance = 3;

    String name;
    Connection connection;
    Duration ping;
    Thread thread;
    State state;
    int entityId;
    UUID uuid;
    Set<Player> nearbyPlayers;
    Position position;
    Inventory inventory;
    Slot selectedItem;
    Set<ChunkLocation> loadedChunks;
    int renderDistance;
    boolean firstSettings = true;

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
            System.out.println("EOF");
        } catch (Exception e) {
            e.printStackTrace();
        }
        disconnect();
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
            for (var chunk : loadedChunks) {
                Main.world.saveChunkSafe(chunk);
            }
        }
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
            case 37 -> handleHeldItemChange(packet);
            case 40 -> handleCreativeInventoryAction(packet);
            case 44 -> handleAnimation(packet);
            case 46 -> handlePlayerPlaceBlock(packet);
            default -> unknownPacket(packet);
        }
    }


    void writeHandshakeResponse() throws IOException {
        connection.sendPacket(0, (p) -> {
            p.writeVarInt(Main.handshake_json.length());
            p.write(Main.handshake_json.getBytes());
        });
    }

    void writePingResponse(long number) throws IOException {
        connection.sendPacket(1, (p) -> {
            var b = ByteBuffer.allocate(Long.BYTES);
            b.order(ByteOrder.BIG_ENDIAN);
            b.putLong(number);
            p.write(b.array());
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
        connection.sendPacket(1, (p) -> { // encryption request
            p.writeString("server id not short");
            var encodedKey = Main.encryptionKey.getPublic().getEncoded();
            p.writeVarInt(encodedKey.length);
            p.writeBytes(encodedKey);
            p.writeVarInt(encryptionToken.length);
            p.writeBytes(encryptionToken);
        });
    }

    void handleLoginStart(Packet packet) throws IOException {
        name = packet.readString();
        uuid = UUID.nameUUIDFromBytes(String.format("OfflinePlayer:%s", name).getBytes());
        // uuid = UUID.randomUUID();
        System.out.format("login: '%s'%n", name);
        sendEncryptionRequest();
    }

    void setCompression(int maxLen) throws IOException {
        connection.sendPacket(3, (p) -> {
            p.writeVarInt(maxLen);
        });
        connection.setCompression(maxLen);
    }

    void sendLoginSuccess() throws IOException {
        connection.sendPacket(2, (p) -> { // login success
            p.writeLong(0);
            p.writeLong(0);
            p.writeString(name);
        });
    }

    void sendBrand(String brand) throws IOException {
        connection.sendPacket(0x17, (p) -> {
            p.writeString("minecraft:brand");
            p.writeBytes(brand.getBytes());
        });
    }

    void sendJoinGame() throws IOException {
        connection.sendPacket(0x24, (p) -> { // Join Game
            p.writeInt(entityId);
            p.writeBoolean(false); // is hardcore
            p.writeByte((byte)1); // gamemode creative
            p.writeByte((byte)-1); // previous gamemode
            p.writeVarInt(1); // world count
            p.writeString("minecraft:overworld"); // list of worlds (# count)
            Main.dimensionCodec.encode(p);
            Main.dimension.encode(p);
            p.writeString("minecraft:overworld"); // world name
            p.writeLong(1); // hashed seed
            p.writeVarInt(100); // max players
            p.writeVarInt(10); // view distance
            p.writeBoolean(false); // reduce debug info
            p.writeBoolean(true); // enable respawn screen
            p.writeBoolean(false); // world is debug (never)
            p.writeBoolean(true); // world is superflat
        });
    }

    void doPreJoin() throws IOException {
        sendJoinGame();
        sendAddPlayers(Main.players);
        sendAddPlayer(this);
        sendBrand("BeforeBrand");
        position = Main.playerLocations.getOrDefault(name, new Position());
        sendUpdateChunkPosition();
        inventory = new Inventory();
        selectedItem = inventory.get(36);
        loadedChunks = new HashSet<>();
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
        connection.sendPacket(0x56, (p) -> {
            p.writeVarInt(entityId);
            p.writeDouble(position.x);
            p.writeDouble(position.y);
            p.writeDouble(position.z);
            p.writeByte(position.yawAngle());
            p.writeByte(position.pitchAngle());
            p.writeBoolean(position.onGround);
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

        setCompression(128);
        System.out.println("Writing Login Success!");
        sendLoginSuccess();
        state = State.Play;
        doPreJoin();
    }

    // handle play packets

    void sendChunk(ChunkLocation chunkLocation) throws IOException {
        connection.sendPacket(0x20, (p) -> {
            p.writeInt(chunkLocation.x());
            p.writeInt(chunkLocation.z());
            Main.world.load(chunkLocation).encodePacket(p);
        });
        loadedChunks.add(chunkLocation);
    }

    void unloadChunk(ChunkLocation chunkLocation) throws IOException {
        connection.sendPacket(0x1c, (p) -> {
            p.writeInt(chunkLocation.x());
            p.writeInt(chunkLocation.z());
        });
        loadedChunks.remove(chunkLocation);
        Main.world.saveChunkSafe(chunkLocation);
    }

    void sendInventory() throws IOException {
        connection.sendPacket(0x13, (p) -> {
            p.writeByte((byte) 0);
            p.writeShort((short) inventory.size());
            for (var slot : inventory.slots) {
                slot.encode(p);
            }
        });
    }

    void sendPositionLook() throws IOException {
        connection.sendPacket(0x34, (p) -> {
            p.writeDouble(position.x);
            p.writeDouble(position.y);
            p.writeDouble(position.z);
            p.writeFloat(position.yaw);
            p.writeFloat(position.pitch);
            p.writeByte((byte) 0);
            p.writeVarInt(0);
        });
    }

    void sendUpdateChunkPosition(int x, int z) throws IOException {
        connection.sendPacket(0x40, (p) -> {
            p.writeVarInt(x);
            p.writeVarInt(z);
        });
    }

    void sendUpdateChunkPosition() throws IOException {
        sendUpdateChunkPosition(position.chunkX(), position.chunkZ());
    }

    void sendSpawnPosition() throws IOException {
        connection.sendPacket(0x42, (p) -> {
            p.writePosition(0, 32, 0);
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
        connection.sendPacket(0x32, (p) -> {
            p.writeVarInt(0);
            p.writeVarInt(players.size());
            for (var player : players) {
                p.writeLong(player.uuid.getMostSignificantBits());
                p.writeLong(player.uuid.getLeastSignificantBits());
                p.writeString(player.name);
                p.writeVarInt(0); // number of properties
                p.writeVarInt(1); // gamemode
                p.writeVarInt(0); // ping
                p.writeBoolean(false); // has display name
            }
        });
    }

    void sendAddPlayer(Player player) throws IOException {
        connection.sendPacket(0x32, (p) -> {
            p.writeVarInt(0);
            p.writeVarInt(1);
            p.writeLong(player.uuid.getMostSignificantBits());
            p.writeLong(player.uuid.getLeastSignificantBits());
            p.writeString(player.name);
            p.writeVarInt(0); // number of properties
            p.writeVarInt(1); // gamemode
            p.writeVarInt(0); // ping
            p.writeBoolean(false); // has display name
        });
    }

    void sendSpawnPlayer(Player player) throws IOException {
        connection.sendPacket(4, (p) -> {
            p.writeVarInt(player.entityId);
            p.writeLong(player.uuid.getMostSignificantBits());
            p.writeLong(player.uuid.getLeastSignificantBits());
            p.writeDouble(player.position.x);
            p.writeDouble(player.position.y);
            p.writeDouble(player.position.z);
            p.writeByte(player.position.yawAngle());
            p.writeByte(player.position.pitchAngle());
        });
    }

    void sendRemovePlayer(Player player) throws IOException {
        connection.sendPacket(0x32, (p) -> {
            p.writeVarInt(4);
            p.writeVarInt(1);
            p.writeLong(player.uuid.getMostSignificantBits());
            p.writeLong(player.uuid.getLeastSignificantBits());
        });
    }

    void sendDespawnPlayer(Player player) throws IOException {
        connection.sendPacket(0x36, (p) -> {
            p.writeVarInt(1);
            p.writeVarInt(player.entityId);
        });
    }

    void sendChat(Player sender, String message) throws IOException {
        var chat = new JSONObject();
        chat.put("text", String.format("<%s> %s", sender.name, message));
        connection.sendPacket(0x0E, (p) -> {
            p.writeString(chat.toString());
            p.writeByte((byte) 0);
            p.writeLong(sender.uuid.getMostSignificantBits());
            p.writeLong(sender.uuid.getLeastSignificantBits());
        });
    }

    void sendSystemMessage(String message, String color) throws IOException {
        var chat = new JSONObject();
        chat.put("text", message);
        chat.put("color", color);
        connection.sendPacket(0x0E, (p) -> {
            p.writeString(chat.toString());
            p.writeByte((byte) 0);
            p.writeLong(0);
            p.writeLong(0);
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
        connection.sendPacket(0x28, (p) -> {
            p.writeVarInt(entityId);
            p.writeShort(protocol_dx);
            p.writeShort(protocol_dy);
            p.writeShort(protocol_dz);
            p.writeByte(position.yawAngle());
            p.writeByte(position.pitchAngle());
            p.writeBoolean(position.onGround);
        });
        connection.sendPacket(0x3A, (p) -> {
            p.writeVarInt(entityId);
            p.writeByte(position.yawAngle());
        });
    }

    void loadCorrectChunks(int chunkX, int chunkZ) throws IOException {
        Set<ChunkLocation> shouldLoad = new HashSet<>();
        for (int cx = chunkX - renderDistance; cx <= chunkX + renderDistance; cx++) {
            for (int cz = chunkZ - renderDistance; cz <= chunkZ + renderDistance; cz++) {
                shouldLoad.add(new ChunkLocation(cx, cz));
            }
        }
        var unloadChunks = new HashSet<>(loadedChunks);
        unloadChunks.removeAll(shouldLoad);
        for (var chunk : unloadChunks) {
            unloadChunk(chunk);
        }
        shouldLoad.removeAll(loadedChunks);
        for (var chunk : shouldLoad) {
            sendChunk(chunk);
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
        connection.sendPacket(5, (p) -> {
            p.writeVarInt(entityId);
            p.writeByte((byte) animation);
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
        connection.sendPacket(0xB, (p) -> {
            p.writeLong(location.encode());
            p.writeVarInt(blockId);
        });
    }

    void handlePlayerDigging(Packet packet) throws IOException {
        var status = packet.readVarInt();
        var location = packet.readLocation();
        var face = packet.read();
        System.out.format("%s Dig %s : %d%n", name, location, status);
        Main.world.setBlock(location, 0);
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
        if (Main.itemToBlock.containsKey(selectedItem.itemId)) {
            var blockId = Main.itemToBlock.get(selectedItem.itemId);
            Main.world.setBlock(location, blockId);
            for (var player : Main.players) {
                player.sendBlockChange(location, blockId);
            }
        } else {
            System.out.printf("Attempt to place %d is invalid%n", selectedItem.itemId);
            sendBlockChange(location, 0);
        }
    }

    // inventory

    void handleCreativeInventoryAction(Packet packet) throws IOException {
        var slotNumber = packet.readShort();
        var slot = Slot.from(packet);
        inventory.put(slotNumber, slot);
        System.out.printf("Inventory %d = %s%n", slotNumber, slot);
    }

    void handleHeldItemChange(Packet packet) throws IOException {
        var slotNumber = packet.readShort();
        selectedItem = inventory.get(slotNumber + 36);
        System.out.printf("Select %d %s%n", slotNumber, selectedItem);
        // TODO send the item to nearbyPlayers
    }
}