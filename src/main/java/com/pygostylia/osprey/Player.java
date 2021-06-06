package com.pygostylia.osprey;

import org.json.JSONObject;

import javax.crypto.Cipher;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class Player extends Entity {
    enum State {
        Status,
        Login,
        Play,
    }

    static final byte[] encryptionToken = "Hello World".getBytes();
    static final int maxRenderDistance = 10;

    String name;
    private final Connection connection;
    Duration ping;
    State state;
    Integer playerId;
    Inventory inventory;
    int selectedHotbarSlot;
    Set<ChunkLocation> loadedChunks;
    int renderDistance;
    boolean firstSettings = true;
    Location[] editorLocations;
    boolean isElytraFlying;
    boolean isCreativeFlying;
    boolean isSneaking;
    boolean isSprinting;
    boolean isShielding;
    int vehicleEntityId;
    Instant startedUsing;
    boolean placeFalling;
    boolean boom;

    Player(Socket sock) throws IOException {
        super();
        connection = new Connection(sock);
        state = State.Status;
    }

    @Override
    int type() {
        return 106;
    }

    static void runThread(Socket sock) throws IOException {
        var player = new Player(sock);
        new Thread(player::connectionWrapper).start();
    }

    void connectionWrapper() {
        try {
            handleConnection();
        } catch (EOFException e) {
            println("EOF");
        } catch (Exception e) {
            println("Error");
            e.printStackTrace();
        }
        disconnect();
    }

    public void kick() throws IOException {
        connection.close();
    }

    private void disconnect() {
        try {
            connection.close();
        } catch (Exception ignored) {}
        Main.removePlayer(this);
        if (connection.isEstablished()) {
            try {
                Main.forEachPlayer((player) -> {
                    player.sendDespawnPlayer(this);
                    player.sendRemovePlayer(this);
                    player.sendNotification(String.format("%s has left the game.", name));
                });
            } catch (IOException e) {
                println("Error telling other players about disconnect");
                e.printStackTrace();
            }
            try {
                saveState();
            } catch (SQLException e) {
                println("Error saving player data");
                e.printStackTrace();
            }
        }
    }

    @Override
    float colliderXZ() {
        return 0.6f;
    }

    @Override
    float colliderY() {
        return 1.8f;
    }

    private void otherPlayers(PlayerIOLambda lambda) throws IOException {
        Main.forEachPlayer((player) -> {
            if (player == this) return;
            lambda.apply(player);
        });
    }

    private void handleConnection() throws IOException {
        while (!connection.isClosed()) {
            try {
                Packet p = connection.readPacket();
                handlePacket(p);
            } catch (SocketException e) {
                printf("Exception");
                e.printStackTrace();
                connection.close();
            }
        }
        println("Leaving handleConnection");
    }

    void teleport(Location location) throws IOException {
        position.moveTo(location);
        teleport();
    }

    void teleport() throws IOException {
        sendPositionLook();
        sendUpdateChunkPosition();
        otherPlayers((player) -> player.sendEntityTeleport(id, position));
        loadCorrectChunks();
    }

    private void handlePacket(Packet packet) throws IOException {
        switch (state) {
            case Status -> handleStatusPacket(packet);
            case Login -> handleLoginPacket(packet);
            case Play -> handlePlayPacket(packet);
        }
    }

    void unknownPacket(Packet packet) {
        printf("Unknown packet type %d %#x in state %s%n", packet.type, packet.type, state);
    }

    private void handleStatusPacket(Packet packet) throws IOException {
        switch (packet.type) {
            case 0 -> handleHandshake(packet);
            case 1 -> handlePing(packet);
            default -> unknownPacket(packet);
        }
    }

    private void handleLoginPacket(Packet packet) throws IOException {
        switch (packet.type) {
            case 0 -> handleLoginStart(packet);
            case 1 -> handleEncryptionResponse(packet);
            default -> unknownPacket(packet);
        }
    }

    private void handlePlayPacket(Packet packet) throws IOException {
        switch (packet.type) {
            case 3 -> handleChat(packet);
            case 4 -> handleClientStatus(packet);
            case 5 -> handleSettings(packet);
            case 11 -> handlePluginMessage(packet);
            case 14 -> handleInteractEntity(packet);
            case 16 -> handleKeepAlive(packet);
            case 18 -> handlePosition(packet);
            case 19 -> handlePositionAndRotation(packet);
            case 20 -> handleRotation(packet);
            case 21 -> handleMovement(packet);
            case 22 -> handleVehicleMove(packet);
            case 23 -> handleSteerBoat(packet);
            case 26 -> handleIsFlying(packet);
            case 27 -> handlePlayerDigging(packet);
            case 28 -> handleEntityAction(packet);
            case 29 -> handleSteerVehicle(packet);
            case 37 -> handleHeldItemChange(packet);
            case 40 -> handleCreativeInventoryAction(packet);
            case 44 -> handleAnimation(packet);
            case 46 -> handlePlayerPlaceBlock(packet);
            case 47 -> handlePlayerUseItem(packet);
            default -> unknownPacket(packet);
        }
    }

    void writeHandshakeResponse() throws IOException {
        final String handshakeInfo = Main.handshakeJson();
        connection.sendPacket(0, (p) -> {
            p.writeVarInt(handshakeInfo.length());
            p.write(handshakeInfo.getBytes());
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

    private void handleHandshake(Packet packet) throws IOException {
        if (packet.originalLen > 2) {
            int protocolVersion = packet.readVarInt();
            String address = packet.readString();
            short port = packet.readShort();
            int next = packet.readVarInt();

            printf("handshake: version: %d address: '%s' port: %d next: %d%n",
                protocolVersion, address, port, next);
            if (next == 2) {
                state = State.Login;
            }
        } else {
            writeHandshakeResponse();
        }
    }

    private void handlePing(Packet packet) throws IOException {
        long number = packet.readLong();
        writePingResponse(number);
    }

    // handle login packets

    public void sendEncryptionRequest() throws IOException {
        connection.sendPacket(1, (p) -> { // encryption request
            p.writeString("server id not short");
            var encodedKey = Main.encryptionKey.getPublic().getEncoded();
            p.writeVarInt(encodedKey.length);
            p.writeBytes(encodedKey);
            p.writeVarInt(encryptionToken.length);
            p.writeBytes(encryptionToken);
        });
    }

    private void handleLoginStart(Packet packet) throws IOException {
        name = packet.readString();
        uuid = UUID.nameUUIDFromBytes(String.format("OfflinePlayer:%s", name).getBytes());
        printf("UUID: %s%n", uuid);
        printf("login: '%s'%n", name);
        sendEncryptionRequest();
    }

    void setCompression(int maxLen) throws IOException {
        connection.sendPacket(3, (p) -> p.writeVarInt(maxLen));
        connection.setCompression(maxLen);
    }

    public void sendLoginSuccess() throws IOException {
        connection.sendPacket(2, (p) -> { // login success
            p.writeLong(0);
            p.writeLong(0);
            p.writeString(name);
        });
    }

    public void sendPluginMessage(String plugin, PacketBuilderLambda inner) throws IOException {
        connection.sendPacket(0x17, (p) -> {
            p.writeString(plugin);
            inner.apply(p);
        });
    }

    public void sendBrand(String brand) throws IOException {
        sendPluginMessage("minecraft:brand", (p) -> p.writeString(brand));
    }

    public void sendJoinGame() throws IOException {
        connection.sendPacket(0x24, (p) -> { // Join Game
            p.writeInt(id);
            p.writeBoolean(false); // is hardcore
            p.writeByte((byte)1); // gamemode creative
            p.writeByte((byte)-1); // previous gamemode
            p.writeVarInt(1); // world count
            p.writeString("minecraft:overworld"); // list of worlds (# count)
            Main.dimensionCodec.write(p);
            Main.overworldDimension.write(p);
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
        loadFromDb();
        sendJoinGame();
        sendCommandData();
        sendAddPlayers(Main.allPlayers());
        sendAddPlayer(this);
        sendBrand(Main.brand);
        sendUpdateChunkPosition();
        loadedChunks = new HashSet<>();
        editorLocations = new Location[2];
        sendInventory();
        sendHeldItemChange((byte) selectedHotbarSlot);
        Main.forEachPlayer((player) -> {
            player.sendAddPlayer(this);
            player.sendSpawnPlayer(this);
            player.sendEquipment(this);
            sendSpawnPlayer(player);
            sendEquipment(player);
        });
        Main.addPlayer(this);
        Main.forEachPlayer((player) -> player.sendNotification(String.format("%s has joined the game (id %d)", name, id)));
    }

    public void sendEntityTeleport(int entityId, Position position) throws IOException {
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

    private void handleEncryptionResponse(Packet packet) throws IOException {
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
            printf("  decrypted token: %s%n",
                Arrays.toString(decryptedToken));

            cipher.init(Cipher.DECRYPT_MODE, Main.encryptionKey.getPrivate());
            decryptedSecret = cipher.doFinal(secret);
            printf("  decrypted secret key: %s%n",
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
            println("Encryption failure!");
            return;
        }

        setCompression(128);
        println("Writing Login Success!");
        sendLoginSuccess();
        state = State.Play;
        doPreJoin();
    }

    // handle play packets

    public void sendChunk(ChunkLocation chunkLocation) throws IOException {
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
    }

    public void sendInventory() throws IOException {
        connection.sendPacket(0x13, (p) -> {
            p.writeByte((byte) 0);
            p.writeShort((short) inventory.size());
            for (int i = 0; i < inventory.size(); i++) {
                inventory.get(i).encode(p);
            }
        });
    }

    public void sendPositionLook() throws IOException {
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

    public void sendUpdateChunkPosition(int x, int z) throws IOException {
        connection.sendPacket(0x40, (p) -> {
            p.writeVarInt(x);
            p.writeVarInt(z);
        });
    }

    public void sendUpdateChunkPosition() throws IOException {
        sendUpdateChunkPosition(position.chunkX(), position.chunkZ());
    }

    public void sendSpawnPosition() throws IOException {
        connection.sendPacket(0x42, (p) -> p.writePosition(0, 32, 0));
    }

    void initialSpawnPlayer() throws IOException {
        sendSpawnPosition();
        loadCorrectChunks();
        sendUpdateChunkPosition();
        sendPositionLook();
    }

    private void handleSettings(Packet packet) throws IOException {
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

    private void handlePluginMessage(Packet packet) throws IOException {
        String channel = packet.readString();
        switch (channel) {
            case "minecraft:brand" -> printf("client brand: %s%n", packet.readString());
            case "worldedit:cui" -> sendCUIEvent(-1, null, 0);
        }
    }

    private void handleKeepAlive(Packet packet) throws IOException {
        long keepAlive = packet.readLong();
        if (!connection.validateKeepAlive(keepAlive)) {
            println("Keepalives did not match!");
        }
        ping = connection.pingTime();
    }

    public void sendAddPlayers(Collection<Player> players) throws IOException {
        if (players.size() == 0) {
            return;
        }
        connection.sendPacket(0x32, (p) -> {
            p.writeVarInt(0);
            p.writeVarInt(players.size());
            for (var player : players) {
                p.writeUUID(player.uuid);
                p.writeString(player.name);
                p.writeVarInt(0); // number of properties
                p.writeVarInt(1); // gamemode
                p.writeVarInt(0); // ping
                p.writeBoolean(false); // has display name
            }
        });
    }

    public void sendAddPlayer(Player player) throws IOException {
        connection.sendPacket(0x32, (p) -> {
            p.writeVarInt(0);
            p.writeVarInt(1);
            p.writeUUID(player.uuid);
            p.writeString(player.name);
            p.writeVarInt(0); // number of properties
            p.writeVarInt(1); // gamemode
            p.writeVarInt(0); // ping
            p.writeBoolean(false); // has display name
        });
    }

    public void sendSpawnPlayer(Player player) throws IOException {
        connection.sendPacket(4, (p) -> {
            p.writeVarInt(player.id);
            p.writeUUID(player.uuid);
            p.writeDouble(player.position.x);
            p.writeDouble(player.position.y);
            p.writeDouble(player.position.z);
            p.writeByte(player.position.yawAngle());
            p.writeByte(player.position.pitchAngle());
        });
    }

    public void sendRemovePlayer(Player player) throws IOException {
        connection.sendPacket(0x32, (p) -> {
            p.writeVarInt(4);
            p.writeVarInt(1);
            p.writeUUID(player.uuid);
        });
    }

    public void sendDespawnPlayer(Player player) throws IOException {
        connection.sendPacket(0x36, (p) -> {
            p.writeVarInt(1);
            p.writeVarInt(player.id);
        });
    }

    public void sendChat(Player sender, String message) throws IOException {
        var chat = new JSONObject();
        chat.put("text", String.format("<%s> %s", sender.name, message));
        connection.sendPacket(0x0E, (p) -> {
            p.writeString(chat.toString());
            p.writeByte((byte) 0);
            p.writeUUID(sender.uuid);
        });
    }

    public void sendSystemMessage(String message, String color) throws IOException {
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

    public void sendNotification(String message) throws IOException {
        sendSystemMessage(message, "yellow");
    }

    public void sendError(String message) throws IOException {
        sendSystemMessage(message, "red");
    }

    public void sendEditorNotification(String message) throws IOException {
        sendSystemMessage(message, "light_purple");
    }

    public void sendSpeed(float speed) throws IOException {
        connection.sendPacket(0x30, (p) -> {
            p.write(0x0F);
            p.writeFloat(speed / 20); // for some reason, 0.05 is "normal speed"
            p.writeFloat(0.1f);
        });
    }

    private void handleChat(Packet packet) throws IOException {
        String message = packet.readString();
        printf("[chat] %s%n", message);
        if (message.startsWith("/")) {
            Main.commands.dispatch(this, message.substring(1).split(" +"));
        }
        else {
            Main.forEachPlayer((player) -> {
                player.sendChat(this, message);
            });
        }
    }

    public void sendCommandData() throws IOException {
        connection.sendPacket(0x10, (p) -> p.write(Main.commandPacket));
    }

    // movement

    public void sendEntityPositionAndRotation(int entityId, double dx, double dy, double dz, Position position) throws IOException {
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

    public void sendEntityVelocity(Entity entity, Velocity velocity) throws IOException {
        connection.sendPacket(0x46, (p) -> {
            p.writeVarInt(entity.id);
            velocity.write(p); // TODO: should this be a subclass and/or Entity.velocity
            // ObjectEntity implements MovableEntity
            // LivingEntity implements MovableEntity
            // Player implements MovableEntity
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
        Main.chunkDispatcher.dispatch(this, shouldLoad.stream()
                .sorted(Comparator.comparingDouble(a -> a.distanceFrom(position.chunkLocation()))));
    }

    boolean isAdmin() {
        return name.equals("tyler569");
    }

    void loadCorrectChunks() throws IOException {
        loadCorrectChunks(position.chunkX(), position.chunkZ());
    }

    void checkChunkPosition(double x, double z) throws IOException {
        int chunkX = (int)x >> 4;
        int chunkZ = (int)z >> 4;
        if (chunkX != position.chunkX() || chunkZ != position.chunkZ()) {
            sendUpdateChunkPosition(chunkX, chunkZ);
            printf("new chunk %d %d%n", chunkX, chunkZ);
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
        otherPlayers((player) -> player.sendEntityPositionAndRotation(id, delta_x, delta_y, delta_z, position));
        if (isElytraFlying && onGround) {
            isElytraFlying = false;
            Main.forEachPlayer((player) -> player.sendPlayerEntityMetadata(this));
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

    private void handlePosition(Packet packet) throws IOException {
        double x = packet.readDouble();
        double y = packet.readDouble();
        double z = packet.readDouble();
        boolean onGround = packet.readBoolean();
        updatePosition(x, y, z, onGround);
    }

    private void handlePositionAndRotation(Packet packet) throws IOException {
        double x = packet.readDouble();
        double y = packet.readDouble();
        double z = packet.readDouble();
        float yaw = packet.readFloat();
        float pitch = packet.readFloat();
        boolean onGround = packet.readBoolean();
        updatePosition(x, y, z, pitch, yaw, onGround);
    }

    public Position eyes() {
        return position.offset(0, 1.6, 0);
    }

    private void handleRotation(Packet packet) throws IOException {
        float yaw = packet.readFloat();
        float pitch = packet.readFloat();
        boolean onGround = packet.readBoolean();
        updatePosition(pitch, yaw, onGround);
    }

    private void handleMovement(Packet packet) throws IOException {
        boolean onGround = packet.readBoolean();
        otherPlayers(player -> player.sendEntityTeleport(id, position));
        updatePosition(onGround);
    }

    boolean intersectsLocation(Location location) {
        if (position.x + 0.3 < location.x() || location.x() + 1 < position.x - 0.3) return false;
        if (position.z + 0.3 < location.z() || location.z() + 1 < position.z - 0.3) return false;
        if (position.y + 1.8 < location.y() || location.y() + 1 <= position.y) return false;

        return true;
    }

    // animation

    public void sendEntityAnimation(int entityId, int animation) throws IOException {
        connection.sendPacket(5, (p) -> {
            p.writeVarInt(entityId);
            p.writeByte((byte) animation);
        });
    }

    public void sendEntityStatus(int entityId, byte status) throws IOException {
        connection.sendPacket(0x1A, (p) -> {
            p.writeInt(entityId);
            p.writeByte(status);
        });
    }

    public void sendSoundEffect(int soundEffect, int category, Location location) throws IOException {
        connection.sendPacket(0x51, (p) -> {
            p.writeVarInt(soundEffect);
            p.writeVarInt(category);
            p.writeInt(location.x());
            p.writeInt(location.y());
            p.writeInt(location.z());
            p.writeFloat(1);
            p.writeFloat(1);
        });
    }

    private void handleEntityAction(Packet packet) throws IOException {
        int entity = packet.readVarInt();
        if (entity != id) {
            println("I got an update for not me");
            return;
        }
        int action = packet.readVarInt();
        switch (action) {
            case 0 -> isSneaking = true;
            case 1 -> isSneaking = false;
            case 3 -> isSprinting = true;
            case 4 -> isSprinting = false;
            case 8 -> isElytraFlying = true;
        }

        otherPlayers(player -> player.sendPlayerEntityMetadata(this));
    }

    private void handleAnimation(Packet packet) throws IOException {
        var hand = packet.readVarInt();
        otherPlayers(player -> player.sendEntityAnimation(this.id, 0));
    }

    public void sendPlayerEntityMetadata(Player player) throws IOException {
        connection.sendPacket(0x44, (p) -> {
            byte flags = 0;
            if (player.isElytraFlying) flags |= 0x80;
            if (player.isSneaking) flags |= 0x02;
            if (player.isSprinting) flags |= 0x08;
            int state = 0;
            if (player.isSneaking) state = 5;
            byte hand = 0;
            if (player.isHoldingShield(1)) {
                hand = 0x02; // "offhand active"
            }
            if (player.isShielding) {
                hand |= 0x01; // "hand active"
            }

            p.writeVarInt(player.id);
            p.writeByte((byte) 0); // base flags
            p.writeVarInt(0); // type: byte
            p.writeByte(flags);
            p.writeByte((byte) 6); // pose
            p.writeVarInt(18); // type: pose
            p.writeVarInt(state);
            p.writeByte((byte) 7); // hand states
            p.writeVarInt(0); // type: byte
            p.writeByte(hand);
            p.writeByte((byte) 0xFF); // end
        });
    }

    public void sendEntityMetadata(Entity entity, int index, int type, Object value) throws IOException {
        connection.sendPacket(0x44, (p) -> {
            p.writeVarInt(entity.id);
            p.writeByte((byte) index);
            p.writeVarInt(type);

            switch (type) {
                case 0 -> p.writeByte((Byte) value);
                case 1 -> p.writeVarInt((Integer) value);
                case 2 -> p.writeFloat((Float) value);
                case 3, 4 -> p.writeString((String) value);
                case 5 -> {
                    if (value == null) {
                        p.write(0);
                    } else {
                        p.write(1);
                        p.write((byte[]) value);
                    }
                }
                case 6 -> p.write((byte[]) value);
                case 9 -> {
                    if (value instanceof Position position) {
                        p.writeLong(position.location().encode());
                    } else if (value instanceof Location location) {
                        p.writeLong(location.encode());
                    } else {
                        throw new IllegalStateException("Illegal Position");
                    }
                }
                case 17 -> {
                    if (value == null) {
                        p.writeVarInt(0);
                    } else {
                        p.writeVarInt((Integer) value + 1);
                    }
                }
                default -> throw new UnsupportedOperationException("To Do");
            }
            p.writeByte((byte) 0xFF); // end
        });
    }

    private void handleSteerVehicle(Packet packet) throws IOException {
        float sideways = packet.readFloat();
        float forward = packet.readFloat();
        int flags = packet.read();

        var vehicle = Main.entityById(vehicleEntityId);
        if (vehicle.isEmpty()) {
            return;
        }

        if ((flags & 0x02) != 0) {
            if (vehicle.get() instanceof BoatEntity boat) {
                boat.removePassenger(this);
                position.moveBy(0, 1, 0);
                teleport();
            }
        }
    }

    // block place & remove

    public void sendBlockChange(Location location, int blockId) throws IOException {
        connection.sendPacket(0xB, (p) -> {
            p.writeLong(location.encode());
            p.writeVarInt(blockId);
        });
    }

    public void sendBlockBreakParticles(Location location, short blockId) throws  IOException {
        connection.sendPacket(0x21, (p) -> {
            p.writeInt(2001);
            p.writePosition(location);
            p.writeInt(blockId);
            p.writeBoolean(false);
        });
    }

    private void handlePlayerDigging(Packet packet) throws IOException {
        var status = packet.readVarInt();
        var location = packet.readLocation();
        var face = packet.read();
        printf("Dig %s : %d%n", location, status);
        switch (status) {
            case 0 -> {
                // "start" digging, which of course means do all of digging
                // for players in creative mode, which is all of them for now.
                var blockId = Main.world.block(location);
                if (selectedItem().itemId == 586) {
                    sendBlockChange(location, blockId);
                    setEditorLocation(0, location);
                    return;
                }
                Main.world.setBlock(location, 0);
                otherPlayers((player) -> {
                    player.sendBlockBreakParticles(location, blockId);
                    player.sendBlockChange(location, 0);
                });
            }
            case 1 -> {
                // cancel digging
            }
            case 2 -> {
                // finish digging
            }
            case 3 -> {
                // drop item stack
            }
            case 4 -> {
                // drop item
            }
            case 5 -> {
                // finish interacting
                isShielding = false;
                Main.forEachPlayer((player) -> {
                    player.sendPlayerEntityMetadata(this);
                });
                if (isHoldingBow(0)) {
                    ArrowEntity arrow = new ArrowEntity(this, eyes(), useTime());
                    arrow.spawn();
                    arrow.explode = boom;
                }
            }
            case 6 -> {
                var held = selectedItem();
                inventory.put((short) (selectedHotbarSlot + 36), offhandItem());
                inventory.put((short) 45, held);
                Main.forEachPlayer((player) -> {
                    // explicitly including this player, this updates the client
                    player.sendEquipment(this);
                });
            }
        }
    }

    private void handlePlayerPlaceBlock(Packet packet) throws IOException {
        var hand = packet.readVarInt();
        var originalLocation = packet.readLocation();
        if (selectedItem().itemId == 586 && hand == 0) {
            setEditorLocation(1, originalLocation);
            return;
        }
        var face = packet.readVarInt();
        final var location = switch (face) {
            case 0 -> originalLocation.offset(0, -1, 0);
            case 1 -> originalLocation.offset(0, 1, 0);
            case 2 -> originalLocation.offset(0, 0, -1);
            case 3 -> originalLocation.offset(0, 0, 1);
            case 4 -> originalLocation.offset(-1, 0, 0);
            case 5 -> originalLocation.offset(1, 0, 0);
            default -> throw new IllegalStateException("Invalid face value");
        };
        var current = Main.world.block(location);
        float cursorX = packet.readFloat();
        float cursorY = packet.readFloat();
        float cursorZ = packet.readFloat();
        boolean insideBlock = packet.readBoolean();

        if (isHoldingFirework(hand)) {
            Position target = new Position(originalLocation);
            target.x += cursorX;
            target.y += cursorY;
            target.z += cursorZ;
            FireworkEntity firework = new FireworkEntity(target, new Velocity(0, 0, 0));
            firework.spawn();
            return;
        }

        if (isHoldingBoat(hand)) {
            Position target = new Position(originalLocation);
            target.x += cursorX;
            target.y += cursorY;
            target.z += cursorZ;
            target.yaw = position.yaw;
            BoatEntity boat = new BoatEntity(target);
            boat.spawn();
            return;
        }

        if (placeFalling) {
            System.out.println("Spawn falling block");
            final int sandBlock = Main.registry.itemToBlockDefault(selectedItem().itemId);
            final Position target = Position.middle(location);
            FallingBlockEntity block = new FallingBlockEntity(target, sandBlock);
            sendBlockChange(location, current);
            block.spawn();
            return;
        }

        printf("Place %d %s %s %f %f %f %b%n", hand, location, face, cursorX, cursorY, cursorZ, insideBlock);

        AtomicBoolean blockPlacement = new AtomicBoolean(false);
        Main.forEachPlayer((player) -> {
            if (player.intersectsLocation(location)) {
                blockPlacement.set(true);
            }
        });
        if (blockPlacement.get()) {
            sendBlockChange(location, current);
            return;
        }

        Slot item = selectedItem();
        Integer blockId = Main.registry.itemToBlockDefault(item.itemId);
        if (blockId == null) {
            printf("Attempt to place %d is invalid%n", item.itemId);
            sendBlockChange(location, Main.world.block(location));
            return;
        }
        Main.world.setBlock(location, blockId);
        Main.forEachPlayer((player) -> player.sendBlockChange(location, blockId));
    }

    boolean isHoldingItem(int item, int hand) {
        if (hand == 0) {
            return selectedItem().itemId == item;
        } else {
            return offhandItem().itemId == item;
        }
    }

    boolean isHoldingShield(int hand) {
        final int shield = 897;
        return isHoldingItem(shield, hand);
    }

    boolean isHoldingFirework(int hand) {
        final int fireworkRocket = 846;
        return isHoldingItem(fireworkRocket, hand);
    }

    boolean isHoldingBoat(int hand) {
        final int boat = 667;
        return isHoldingItem(boat, hand);
    }

    boolean isHoldingBow(int hand) {
        final int bow = 574;
        return isHoldingItem(bow, hand);
    }

    private void handlePlayerUseItem(Packet packet) throws IOException {
        int hand = packet.readVarInt();
        printf("Use %d %s%n", hand, selectedItem());

        if (isHoldingShield(hand)) {
            isShielding = true;
            Main.forEachPlayer((player) -> {
                player.sendPlayerEntityMetadata(this);
            });
        }

        if (isHoldingFirework(hand)) {
            FireworkEntity firework = new FireworkEntity(position, Velocity.zero());
            firework.spawnWithRider(id);
        }

        if (isHoldingBow(hand)) {
            startedUsing = Instant.now();
        }
    }

    Duration useTime() {
        return Duration.between(startedUsing, Instant.now());
    }

    // inventory

    private void handleCreativeInventoryAction(Packet packet) throws IOException {
        var slotNumber = packet.readShort();
        var slot = Slot.from(packet);
        inventory.put(slotNumber, slot);
        printf("Inventory %d = %s%n", slotNumber, slot);
        if (slotNumber == selectedHotbarSlot + 36 ||
            slotNumber == 45 ||
            slotNumber >= 5 && slotNumber <= 8
        ) {
            otherPlayers((player) -> player.sendEquipment(this));
        }
    }

    private void handleHeldItemChange(Packet packet) throws IOException {
        selectedHotbarSlot = packet.readShort();
        printf("Select %d %s%n", selectedHotbarSlot,selectedItem());
        otherPlayers(player -> player.sendEquipment(this));
    }

    Slot selectedItem() {
        return inventory.get(selectedHotbarSlot + 36);
    }

    Slot offhandItem() {
        return inventory.get(45);
    }

    public void sendHeldItemChange(byte slot) throws IOException {
        connection.sendPacket(0x3F, (p) -> {
            p.writeByte(slot);
        });
    }

    // log

    void println(Object o) {
        System.out.printf("[%d %s] %s%n", id, name, o);
    }

    void printf(String s, Object... o) {
        System.out.printf("[%d %s] %s", id, name, String.format(s, o));
    }

    // worldedit

    long selectionVolume() {
        if (editorLocations[0] == null || editorLocations[1] == null) {
            return 0;
        }
        return ((long) Math.abs(editorLocations[0].x() - editorLocations[1].x()) + 1) *
            ((long) Math.abs(editorLocations[0].y() - editorLocations[1].y()) + 1) *
            ((long) Math.abs(editorLocations[0].z() - editorLocations[1].z()) + 1);
    }

    void setEditorLocation(int n, Location location) throws IOException {
        editorLocations[n] = location;
        String message = String.format("Position %d set to ", n + 1);
        var volume = selectionVolume();
        if (volume == 0) {
            sendEditorNotification(String.format("%s %s", message, editorLocations[n]));
        } else {
            sendEditorNotification(String.format(
                "%s %s (%d block%s)",
                message,
                editorLocations[n],
                volume,
                volume == 1 ? "" : "s"
            ));
        }
        sendCUIEvent(n, location, selectionVolume());
    }

    void unsetEditorSelection() throws IOException {
        editorLocations[0] = null;
        editorLocations[1] = null;
        sendCUIEvent(-1, null, 0);
        sendEditorNotification("Selection cleared");
    }

    public void sendCUIEvent(int selection, Location location, long volume) throws IOException {
        if (selection == -1) {
            sendPluginMessage("worldedit:cui", (p) -> {
                p.writeBytes("s|cuboid".getBytes());
            });
        } else {
            sendPluginMessage("worldedit:cui", (p) -> {
                p.writeBytes(
                    String.format(
                        "p|%d|%d|%d|%d|%d",
                        selection,
                        location.x(),
                        location.y(),
                        location.z(),
                        volume
                    ).getBytes()
                );
            });
        }
    }

    // persistence

    void loadFromDb() throws IOException {
        String sql = """
            SELECT id, selected_slot, x, y, z, pitch, yaw FROM players WHERE name = ?;
            """;
        try (var connection = Main.world.connect();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            var results = statement.executeQuery();
            if (!results.isClosed()) {
                playerId = results.getInt(1);
                selectedHotbarSlot = results.getInt(2);
                position = new Position();
                var xTmp = results.getDouble(3);
                if (!results.wasNull()) {
                    position.x = xTmp;
                    position.y = results.getDouble(4);
                    position.z = results.getDouble(5);
                    position.pitch = results.getFloat(6);
                    position.yaw = results.getFloat(7);
                }
            }
        } catch (SQLException e) {
            println("Error retrieving player data");
            e.printStackTrace();
            connection.close();
            return;
        }
        if (playerId == null) {
            saveFirstTime();
            loadFromDb();
        }
        try {
            inventory = Inventory.loadFromDb(playerId);
        } catch (SQLException e) {
            println("Error loading inventory");
            e.printStackTrace();
        }
    }

    void saveFirstTime() {
        String sql = """
            INSERT INTO players (uuid, name)
            VALUES (?, ?);
            """;
        try (var connection = Main.world.connect();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.execute();
            connection.commit();
        } catch (SQLException e) {
            println("Error saving player record");
            e.printStackTrace();
        }
    }

    void saveState() throws SQLException {
        String sql = """
            UPDATE players
            SET
                selected_slot = ?,
                x = ?,
                y = ?,
                z = ?,
                pitch = ?,
                yaw = ?
            WHERE id = ?;
            """;
        try (var connection = Main.world.connect();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, selectedHotbarSlot);
            statement.setDouble(2, position.x);
            statement.setDouble(3, position.y);
            statement.setDouble(4, position.z);
            statement.setDouble(5, position.pitch);
            statement.setDouble(6, position.yaw);
            statement.setInt(7, playerId);
            statement.execute();
            connection.commit();
        }
        inventory.save(playerId);
    }

    // change game state

    public void sendChangeGameState(int reason, float value) throws IOException {
        connection.sendPacket(0x1D, (p) -> {
            p.writeByte((byte) reason);
            p.writeFloat(value);
        });
    }

    public void sendChangeGamemode(Player player, int mode) throws IOException {
        connection.sendPacket(0x32, (p) -> {
            p.writeVarInt(1);
            p.writeVarInt(1);
            p.writeUUID(player.uuid);
            p.writeVarInt(mode);
        });
    }
    
    void changeGamemode(int mode) throws IOException {
        sendChangeGameState(3, mode);
        Main.forEachPlayer((player) -> {
            player.sendChangeGamemode(this, mode);
        });
    }

    // spawn entity

    public void sendSpawnEntity(ObjectEntity entity, int data) throws IOException {
        connection.sendPacket(0, (p) -> {
            p.writeVarInt(entity.id);
            p.writeUUID(entity.uuid);
            p.writeVarInt(entity.type());
            p.writePosition(entity.position);
            p.writeInt(data); // TODO data
            entity.velocity.write(p);
        });
    }

    public void sendSpawnEntity(ObjectEntity entity) throws IOException {
        sendSpawnEntity(entity, 0);
    }

    public void sendDestroyEntity(Entity entity) throws IOException {
        connection.sendPacket(0x36, (p) -> {
            p.writeVarInt(1); // count
            p.writeVarInt(entity.id);
        });
    }

    // equipment

    public void sendEquipment(Player player) throws IOException {
        connection.sendPacket(0x47, (p) -> {
            p.writeVarInt(player.id);
            p.writeByte((byte) 0x80); // Main hand
            player.selectedItem().encode(p);
            p.writeByte((byte) 0x81); // Off hand
            player.offhandItem().encode(p);
            p.writeByte((byte) 0x82); // Boots
            player.inventory.get(8).encode(p);
            p.writeByte((byte) 0x83); // Leggings
            player.inventory.get(7).encode(p);
            p.writeByte((byte) 0x84); // Chest
            player.inventory.get(6).encode(p);
            p.writeByte((byte) 0x05); // Helmet
            player.inventory.get(5).encode(p);
        });
    }

    //

    Direction facing() {
        if (position.pitch > 45) {
            return Direction.Down;
        } else if (position.pitch < -45) {
            return Direction.Up;
        }
        var yaw = (position.yaw + 45.0) % 360.0;
        if (yaw < 0) yaw += 360;
        if (yaw < 90) {
            return Direction.South;
        } else if (yaw < 180) {
            return Direction.West;
        } else if (yaw < 270) {
            return Direction.North;
        } else {
            return Direction.East;
        }
    }

    //

    Set<Player> nearbyPlayers() {
        return null;
    }

    //

    // maybe
    private void vehicle(Function<Entity, Void> lambda) {
        Optional<Entity> maybeVehicle = Main.entityById(vehicleEntityId);
        if (maybeVehicle.isEmpty()) return;
        lambda.apply(maybeVehicle.get());
    }

    private void handleInteractEntity(Packet packet) throws IOException {
        int entityId = packet.readVarInt();
        int type = packet.readVarInt();
        if (type == 2) {
            var x = packet.readFloat();
            var y = packet.readFloat();
            var z = packet.readFloat();
        }
        if (type == 0 || type == 2) {
            var hand = packet.readVarInt();
        }
        var sneak = packet.readBoolean();
        printf("Interact %d entity %d%n", type, entityId);

        var target = Main.entityById(entityId);
        if (target.isEmpty()) {
            return;
        }
        switch (type) {
            case 0 -> target.get().interact(this);
            case 1 -> target.get().attack(this);
        }
    }

    public void setRidingEntity(Entity ridingEntity) {
        this.vehicleEntityId = ridingEntity.id;
    }

    public void setNotRidingEntity() {
        this.vehicleEntityId = -1;
    }

    public void sendSetPassengers(Entity entity, Collection<Entity> passengers) throws IOException {
        connection.sendPacket(0x4B, (p) -> {
            p.writeVarInt(entity.id);
            p.writeVarInt(passengers.size());
            for (Entity passenger : passengers) {
                p.writeVarInt(passenger.id);
            }
        });
    }

    private void handleVehicleMove(Packet packet) throws IOException {
        Position position = packet.readPosition();
        this.position = position;
        var vehicle = Main.entityById(vehicleEntityId);
        if (vehicle.isEmpty()) {
            return;
        }
        vehicle.get().position = position;
        otherPlayers(player -> player.sendEntityTeleport(vehicleEntityId, position));
    }

    private void handleSteerBoat(Packet packet) throws IOException {
        boolean left = packet.readBoolean();
        boolean right = packet.readBoolean();
        var vehicle = Main.entityById(vehicleEntityId);
        if (vehicle.isEmpty()) {
            return;
        }
        if (vehicle.get() instanceof BoatEntity boat) {
            boat.turningLeft = left;
            boat.turningRight = right;
            otherPlayers(player -> player.sendEntityMetadata(boat, 11, 7, left));
            otherPlayers(player -> player.sendEntityMetadata(boat, 12, 7, right));
        }
    }

    public void sendGameOver(Player killer) throws IOException {
        var chat = new JSONObject();
        chat.put("text", "Oof");
        connection.sendPacket(0x31, (p) -> {
            p.writeVarInt(2);
            p.writeVarInt(id);
            p.writeInt(killer.id);
            p.writeString(chat.toString());
        });
    }

    private void handleClientStatus(Packet packet) throws IOException {
        var action = packet.readVarInt();
        if (action == 0) { // perform respawn
        }
    }

    private void handleIsFlying(Packet packet) throws IOException {
        var status = packet.read();
        isCreativeFlying = (status & 0x02) != 0;
        isElytraFlying = false;
        Main.forEachPlayer((player) -> {
            player.sendPlayerEntityMetadata(this);
        });
    }

    public void sendExplosion(Position position, float strength, Collection<Location> boomBlocks, Velocity playerKick) throws IOException {
        connection.sendPacket(0x1B, (p) -> {
            p.writeFloat((float) position.x);
            p.writeFloat((float) position.y);
            p.writeFloat((float) position.z);
            p.writeFloat(strength);
            p.writeInt(boomBlocks.size());
            Location center = position.location();
            for (var block : boomBlocks) {
                p.writeByte((byte) (block.x() - center.x()));
                p.writeByte((byte) (block.y() - center.y()));
                p.writeByte((byte) (block.z() - center.z()));
            }
            p.writeFloat(playerKick.x());
            p.writeFloat(playerKick.y());
            p.writeFloat(playerKick.z());
        });
    }
}