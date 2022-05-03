package com.pygostylia.osprey.entities;

import com.pygostylia.osprey.*;
import com.pygostylia.osprey.packets.clientbound.ClientBoundPacket;
import com.pygostylia.osprey.packets.clientbound.SpawnEntityPacket;
import org.json.JSONObject;

import javax.crypto.Cipher;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class Player extends Entity {
    enum State {
        Status,
        Login,
        Play,
    }

    static final byte[] encryptionToken = "Hello World".getBytes();
    static final int maxRenderDistance = 10;

    public String name;
    private final Connection connection;
    Duration ping;
    State state;
    Integer playerId;
    Inventory inventory;
    int selectedHotbarSlot;
    Set<ChunkPosition> loadedChunks = ConcurrentHashMap.newKeySet();
    Set<ChunkPosition> dispatchedChunks = ConcurrentHashMap.newKeySet();
    int renderDistance = 10;

    BlockPosition[] editorBlockPositions = new BlockPosition[2];
    boolean isElytraFlying;

    boolean isCreativeFlying;
    boolean isSneaking;
    boolean isSprinting;
    boolean isShielding;
    int vehicleEntityId;
    Instant startedUsing;

    // fun stuff
    public boolean placeFalling;
    public boolean boom;
    public boolean bulletTime;
    Map<Integer, ScheduledFuture<?>> futures = new HashMap<>();
    private int nextFuture = 1;

    Player(Socket sock) throws IOException {
        super();
        connection = new Connection(sock);
        state = State.Status;
    }

    public int addFuture(ScheduledFuture<?> future) {
        var index = nextFuture++;
        futures.put(index, future);
        sendNotification("Job submitted as " + index);
        return index;
    }

    public ScheduledFuture<?> removeFuture(int id) {
        return futures.remove(id);
    }

    @Override
    public int type() {
        return 106;
    }

    public String name() {
        return name;
    }

    public Duration ping() {
        return ping;
    }

    public BlockPosition[] getEditorLocations() {
        return editorBlockPositions;
    }

    public boolean isElytraFlying() {
        return isElytraFlying;
    }

    public boolean isCreativeFlying() {
        return isCreativeFlying;
    }

    public boolean isSneaking() {
        return isSneaking;
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public boolean isShielding() {
        return isShielding;
    }

    public EntityPosition entityPosition() {
        return entityPosition;
    }

    public static void runThread(Socket sock) throws IOException {
        var player = new Player(sock);
        var thread = new Thread(player::connectionWrapper);
        thread.start();
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

    public void kick() {
        kick("You were kicked from the server");
    }

    public void kick(String message) {
        var chat = new JSONObject();
        chat.put("text", message);
        chat.put("color", "white");
        sendPacket(0x19, p -> p.writeString(chat.toString()));
        disconnect();
    }

    private void sendPacket(int type, PacketBuilderLambda closure) {
        try {
            connection.sendPacket(type, closure);
        } catch (IOException e) {
            printf("Failed to send packet type %#02x%n", type);
            e.printStackTrace();
            disconnect();
        }
    }

    private void disconnect() {
        connection.close();
        Main.removePlayer(this);
        if (connection.isEstablished()) {
            Main.forEachPlayer(player -> {
                player.sendDespawnPlayer(this);
                player.sendRemovePlayer(this);
                player.sendNotification(String.format("%s has left the game.", name));
            });
            try {
                saveState();
            } catch (SQLException e) {
                println("Error saving player data");
                e.printStackTrace();
            }
        }
    }

    @Override
    public float colliderXZ() {
        return 0.6f;
    }

    @Override
    public float colliderY() {
        return 1.8f;
    }

    private void otherPlayers(Consumer<Player> lambda) {
        Main.forEachPlayer(player -> {
            if (player == this) return;
            lambda.accept(player);
        });
    }

    private void handleConnection() throws IOException {
        while (!connection.isClosed()) {
            try {
                MinecraftInputStream p = connection.readPacket();
                handlePacket(p);
            } catch (SocketException e) {
                printf("Exception! %s;  Last packet was %#02x%n", e.getMessage(), connection.lastPacketType);
                e.printStackTrace();
                connection.close();
            }
        }
        println("Leaving handleConnection");
    }

    public void teleport(BlockPosition blockPosition) {
        entityPosition.moveTo(blockPosition);
        teleport();
    }

    public void teleport() {
        sendPositionLook();
        sendUpdateChunkPosition();
        otherPlayers(player -> player.sendEntityTeleport(id, entityPosition));
        loadCorrectChunks();
    }

    private void handlePacket(MinecraftInputStream packet) throws IOException {
        switch (state) {
            case Status -> handleStatusPacket(packet);
            case Login -> handleLoginPacket(packet);
            case Play -> handlePlayPacket(packet);
        }
    }

    void unknownPacket(MinecraftInputStream packet) {
        printf("Unknown packet type %d %#x in state %s%n", packet.type, packet.type, state);
    }

    private void handleStatusPacket(MinecraftInputStream packet) throws IOException {
        switch (packet.type) {
            case 0 -> handleHandshake(packet);
            case 1 -> handlePing(packet);
            default -> unknownPacket(packet);
        }
    }

    private void handleLoginPacket(MinecraftInputStream packet) throws IOException {
        switch (packet.type) {
            case 0 -> handleLoginStart(packet);
            case 1 -> handleEncryptionResponse(packet);
            default -> unknownPacket(packet);
        }
    }

    private void handlePlayPacket(MinecraftInputStream packet) throws IOException {
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
            case 27 -> handlePlayerAction(packet);
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

    void writeHandshakeResponse() {
        final String handshakeInfo = Main.handshakeJson();
        sendPacket(0, p -> {
            p.writeVarInt(handshakeInfo.length());
            p.write(handshakeInfo.getBytes());
        });
    }

    void writePingResponse(long number) {
        sendPacket(1, p -> {
            var b = ByteBuffer.allocate(Long.BYTES);
            b.order(ByteOrder.BIG_ENDIAN);
            b.putLong(number);
            p.write(b.array());
        });
    }

    // handle Status packets

    private void handleHandshake(MinecraftInputStream packet) throws IOException {
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

    private void handlePing(MinecraftInputStream packet) throws IOException {
        long number = packet.readLong();
        writePingResponse(number);
    }

    // handle login packets

    public void sendEncryptionRequest() {
        sendPacket(1, p -> { // encryption request
            p.writeString("server id not short");
            var encodedKey = Main.encryptionKey.getPublic().getEncoded();
            p.writeVarInt(encodedKey.length);
            p.write(encodedKey);
            p.writeVarInt(encryptionToken.length);
            p.write(encryptionToken);
        });
    }

    private void handleLoginStart(MinecraftInputStream packet) throws IOException {
        name = packet.readString();
        uuid = UUID.nameUUIDFromBytes(String.format("OfflinePlayer:%s", name).getBytes());
        printf("UUID: %s%n", uuid);
        printf("login: '%s'%n", name);
        sendEncryptionRequest();
    }

    void setCompression(int maxLen) {
        sendPacket(3, p -> p.writeVarInt(maxLen));
        connection.setCompression(maxLen);
    }

    public void sendLoginSuccess() {
        sendPacket(2, p -> { // login success
            p.writeLong(0);
            p.writeLong(0);
            p.writeString(name);
        });
    }

    public void sendPluginMessage(String plugin, PacketBuilderLambda inner) {
        sendPacket(0x17, p -> {
            p.writeString(plugin);
            inner.apply(p);
        });
    }

    public void sendBrand(String brand) {
        sendPluginMessage("minecraft:brand", p -> p.writeString(brand));
    }

    public void sendJoinGame() {
        sendPacket(0x24, p -> { // Join Game
            p.writeInt(id);
            p.writeBoolean(false); // is hardcore
            p.writeByte((byte) 1); // gamemode creative
            p.writeByte((byte) -1); // previous gamemode
            p.writeVarInt(1); // world count
            p.writeString("minecraft:overworld"); // list of worlds (# count)
            DimensionCodec.codec.write(p);
            DimensionCodec.overworld.write(p);
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

    void doPreJoin() {
        loadFromDb();
        sendJoinGame();
        sendCommandData();
        sendAddPlayers(Main.allPlayers());
        sendAddPlayer(this);
        sendBrand(Main.brand);
        sendUpdateChunkPosition();
        sendInventory();
        sendTags();
        sendHeldItemChange((byte) selectedHotbarSlot);
        Main.forEachPlayer(player -> {
            player.sendAddPlayer(this);
            player.sendSpawnPlayer(this);
            player.sendEquipment(this);
            sendSpawnPlayer(player);
            sendEquipment(player);
        });
        Main.addPlayer(this);
        Main.forEachPlayer(player -> player.sendNotification(String.format("%s has joined the game (id %d)", name, id)));
        initialSpawnPlayer();
    }

    public void sendEntityTeleport(int entityId, EntityPosition entityPosition) {
        sendPacket(0x56, p -> {
            p.writeVarInt(entityId);
            p.writeDouble(entityPosition.x);
            p.writeDouble(entityPosition.y);
            p.writeDouble(entityPosition.z);
            p.writeByte(entityPosition.yawAngle());
            p.writeByte(entityPosition.pitchAngle());
            p.writeBoolean(entityPosition.onGround);
        });
    }

    private void handleEncryptionResponse(MinecraftInputStream packet) throws IOException {
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

    public void sendInventory() {
        sendPacket(0x13, p -> {
            p.writeByte((byte) 0);
            p.writeShort((short) inventory.size());
            for (int i = 0; i < inventory.size(); i++) {
                inventory.get(i).encode(p);
            }
        });
    }

    public void sendPositionLook() {
        sendPacket(0x34, p -> {
            p.writeDouble(entityPosition.x);
            p.writeDouble(entityPosition.y);
            p.writeDouble(entityPosition.z);
            p.writeFloat(entityPosition.yaw);
            p.writeFloat(entityPosition.pitch);
            p.writeByte((byte) 0);
            p.writeVarInt(0);
        });
    }

    public void sendUpdateChunkPosition(int x, int z) {
        sendPacket(0x40, p -> {
            p.writeVarInt(x);
            p.writeVarInt(z);
        });
    }

    public void sendUpdateChunkPosition() {
        sendUpdateChunkPosition(entityPosition.chunkX(), entityPosition.chunkZ());
    }

    public void sendSpawnPosition() {
        sendPacket(0x42, p -> p.writePosition(0, 32, 0));
    }

    void initialSpawnPlayer() {
        sendSpawnPosition();
        sendPositionLook();
        sendUpdateChunkPosition();
        loadCorrectChunks();
    }

    private void handleSettings(MinecraftInputStream packet) throws IOException {
        String locale = packet.readString();
        renderDistance = packet.read();
        if (renderDistance > maxRenderDistance) renderDistance = maxRenderDistance;
        int chatMode = packet.readVarInt();
        boolean chatColors = packet.readBoolean();
        int skinParts = packet.read();
        int mainHand = packet.readVarInt();
        loadCorrectChunks();
    }

    private void handlePluginMessage(MinecraftInputStream packet) throws IOException {
        String channel = packet.readString();
        switch (channel) {
            case "minecraft:brand" -> printf("client brand: %s%n", packet.readString());
            case "worldedit:cui" -> sendCUIEvent(-1, null, 0);
        }
    }

    private void handleKeepAlive(MinecraftInputStream packet) throws IOException {
        long keepAlive = packet.readLong();
        if (!connection.validateKeepAlive(keepAlive)) {
            println("Keepalives did not match!");
        }
        ping = connection.pingTime();
    }

    static byte[] tagsPacketArray = null;

    static {
        try {
            tagsPacketArray = Files.readAllBytes(Path.of("tagsPacket.dat"));
        } catch (IOException ignored) {
        }
    }

    public void sendTags() {
        if (tagsPacketArray != null) {
            sendPacket(0x5B, p -> {
                p.write(tagsPacketArray);
            });
        }
    }

    public void sendAddPlayers(Collection<Player> players) {
        if (players.size() == 0) {
            return;
        }
        sendPacket(0x32, p -> {
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

    public void sendAddPlayer(Player player) {
        sendPacket(0x32, p -> {
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

    public void sendSpawnPlayer(Player player) {
        sendPacket(4, p -> {
            p.writeVarInt(player.id);
            p.writeUUID(player.uuid);
            p.writeDouble(player.entityPosition.x);
            p.writeDouble(player.entityPosition.y);
            p.writeDouble(player.entityPosition.z);
            p.writeByte(player.entityPosition.yawAngle());
            p.writeByte(player.entityPosition.pitchAngle());
        });
    }

    public void sendRemovePlayer(Player player) {
        sendPacket(0x32, p -> {
            p.writeVarInt(4);
            p.writeVarInt(1);
            p.writeUUID(player.uuid);
        });
    }

    public void sendDespawnPlayer(Player player) {
        sendPacket(0x36, p -> {
            p.writeVarInt(1);
            p.writeVarInt(player.id);
        });
    }

    public void sendChat(Player sender, String message) {
        var chat = new JSONObject();
        chat.put("text", String.format("<%s> %s", sender.name, message));
        sendPacket(0x0E, p -> {
            p.writeString(chat.toString());
            p.writeByte((byte) 0);
            p.writeUUID(sender.uuid);
        });
    }

    public void sendSystemMessage(String message, String color) {
        var chat = new JSONObject();
        chat.put("text", message);
        chat.put("color", color);
        sendPacket(0x0E, p -> {
            p.writeString(chat.toString());
            p.writeByte((byte) 0);
            p.writeLong(0);
            p.writeLong(0);
        });
    }

    public void sendNotification(String message) {
        sendSystemMessage(message, "yellow");
    }

    public void sendError(String message) {
        sendSystemMessage(message, "red");
    }

    public void sendEditorNotification(String message) {
        sendSystemMessage(message, "light_purple");
    }

    public void sendSpeed(float speed) {
        sendPacket(0x30, p -> {
            p.write(0x0F);
            p.writeFloat(speed / 20); // for some reason, 0.05 is "normal speed"
            p.writeFloat(0.1f);
        });
    }

    private void handleChat(MinecraftInputStream packet) throws IOException {
        String message = packet.readString();
        printf("[chat] %s%n", message);
        if (message.startsWith("/")) {
            Main.commands.dispatch(this, message.substring(1).split(" +"));
        } else {
            Main.forEachPlayer(player -> {
                player.sendChat(this, message);
            });
        }
    }

    public void sendCommandData() {
        sendPacket(0x10, p -> p.write(Main.commands.getPacket()));
    }

    // movement

    public void sendEntityPositionAndRotation(int entityId, double dx, double dy, double dz, EntityPosition entityPosition) {
        short protocol_dx = (short) (dx * 4096);
        short protocol_dy = (short) (dy * 4096);
        short protocol_dz = (short) (dz * 4096);
        sendPacket(0x28, p -> {
            p.writeVarInt(entityId);
            p.writeShort(protocol_dx);
            p.writeShort(protocol_dy);
            p.writeShort(protocol_dz);
            p.writeByte(entityPosition.yawAngle());
            p.writeByte(entityPosition.pitchAngle());
            p.writeBoolean(entityPosition.onGround);
        });
        sendPacket(0x3A, p -> {
            p.writeVarInt(entityId);
            p.writeByte(entityPosition.yawAngle());
        });
    }

    public void sendEntityVelocity(Entity entity, Velocity velocity) {
        sendPacket(0x46, p -> {
            p.writeVarInt(entity.id);
            velocity.write(p); // TODO: should this be a subclass and/or Entity.velocity
            // ObjectEntity implements MovableEntity
            // LivingEntity implements MovableEntity
            // Player implements MovableEntity
        });
    }

    public void sendChunk(ChunkPosition chunkPosition) {
        sendPacket(0x20, p -> {
            p.writeInt(chunkPosition.x());
            p.writeInt(chunkPosition.z());
            Main.world.load(chunkPosition).encodePacket(p);
        });
        loadedChunks.add(chunkPosition);
        dispatchedChunks.remove(chunkPosition);
    }

    void unloadChunk(ChunkPosition chunkPosition) {
        sendPacket(0x1c, p -> {
            p.writeInt(chunkPosition.x());
            p.writeInt(chunkPosition.z());
        });
        loadedChunks.remove(chunkPosition);
        dispatchedChunks.remove(chunkPosition);
        // TODO: cancel any pending dispatches in ChunkDispatcher
    }

    void loadCorrectChunks(int chunkX, int chunkZ) {
        Set<ChunkPosition> shouldLoad = new HashSet<>();
        for (int cx = chunkX - renderDistance; cx <= chunkX + renderDistance; cx++) {
            for (int cz = chunkZ - renderDistance; cz <= chunkZ + renderDistance; cz++) {
                shouldLoad.add(new ChunkPosition(cx, cz));
            }
        }
        var unloadChunks = new HashSet<>(loadedChunks);
        unloadChunks.removeAll(shouldLoad);
        for (var chunk : unloadChunks) {
            unloadChunk(chunk);
        }
        shouldLoad.removeAll(loadedChunks);
        shouldLoad.removeAll(dispatchedChunks);
        dispatchedChunks.addAll(shouldLoad);
        Main.chunkDispatcher.dispatch(this, shouldLoad.stream()
                .sorted(Comparator.comparingDouble(a -> a.distanceFrom(entityPosition.chunkLocation()))));
    }

    public boolean isAdmin() {
        return name.equals("tyler569");
    }

    void loadCorrectChunks() {
        loadCorrectChunks(entityPosition.chunkX(), entityPosition.chunkZ());
    }

    void checkChunkPosition(double x, double z) {
        int chunkX = (int) x >> 4;
        int chunkZ = (int) z >> 4;
        if (chunkX != entityPosition.chunkX() || chunkZ != entityPosition.chunkZ()) {
            sendUpdateChunkPosition(chunkX, chunkZ);
            printf("new chunk %d %d%n", chunkX, chunkZ);
            loadCorrectChunks(chunkX, chunkZ);
        }
    }

    void updatePosition(double x, double y, double z, float pitch, float yaw, boolean onGround) {
        double delta_x = x - entityPosition.x;
        double delta_y = y - entityPosition.y;
        double delta_z = z - entityPosition.z;
        checkChunkPosition(x, z);
        entityPosition.x = x;
        entityPosition.y = y;
        entityPosition.z = z;
        entityPosition.pitch = pitch;
        entityPosition.yaw = yaw;
        entityPosition.onGround = onGround;
        otherPlayers(player -> player.sendEntityPositionAndRotation(id, delta_x, delta_y, delta_z, entityPosition));
        if (isElytraFlying && onGround) {
            isElytraFlying = false;
            Main.forEachPlayer(player -> player.sendPlayerEntityMetadata(this));
        }
    }

    void updatePosition(double x, double y, double z, boolean onGround) {
        updatePosition(x, y, z, entityPosition.pitch, entityPosition.yaw, onGround);
    }

    void updatePosition(float pitch, float yaw, boolean onGround) {
        updatePosition(entityPosition.x, entityPosition.y, entityPosition.z, pitch, yaw, onGround);
    }

    void updatePosition(boolean onGround) {
        updatePosition(entityPosition.x, entityPosition.y, entityPosition.z, entityPosition.pitch, entityPosition.yaw, onGround);
    }

    private void handlePosition(MinecraftInputStream packet) throws IOException {
        double x = packet.readDouble();
        double y = packet.readDouble();
        double z = packet.readDouble();
        boolean onGround = packet.readBoolean();
        updatePosition(x, y, z, onGround);
    }

    private void handlePositionAndRotation(MinecraftInputStream packet) throws IOException {
        double x = packet.readDouble();
        double y = packet.readDouble();
        double z = packet.readDouble();
        float yaw = packet.readFloat();
        float pitch = packet.readFloat();
        boolean onGround = packet.readBoolean();
        updatePosition(x, y, z, pitch, yaw, onGround);
    }

    public EntityPosition eyes() {
        return entityPosition.offset(0, 1.6, 0);
    }

    private void handleRotation(MinecraftInputStream packet) throws IOException {
        float yaw = packet.readFloat();
        float pitch = packet.readFloat();
        boolean onGround = packet.readBoolean();
        updatePosition(pitch, yaw, onGround);
    }

    private void handleMovement(MinecraftInputStream packet) throws IOException {
        boolean onGround = packet.readBoolean();
        otherPlayers(player -> player.sendEntityTeleport(id, entityPosition));
        updatePosition(onGround);
    }

    boolean intersectsLocation(BlockPosition blockPosition) {
        if (entityPosition.x + 0.3 < blockPosition.x() || blockPosition.x() + 1 < entityPosition.x - 0.3) return false;
        if (entityPosition.z + 0.3 < blockPosition.z() || blockPosition.z() + 1 < entityPosition.z - 0.3) return false;
        return !(entityPosition.y + 1.8 < blockPosition.y()) && !(blockPosition.y() + 1 <= entityPosition.y);
    }

    // animation

    public void sendEntityAnimation(int entityId, int animation) {
        sendPacket(5, p -> {
            p.writeVarInt(entityId);
            p.writeByte((byte) animation);
        });
    }

    public void sendEntityStatus(int entityId, byte status) {
        sendPacket(0x1A, p -> {
            p.writeInt(entityId);
            p.writeByte(status);
        });
    }

    public void sendSoundEffect(int soundEffect, int category, BlockPosition blockPosition) {
        sendPacket(0x51, p -> {
            p.writeVarInt(soundEffect);
            p.writeVarInt(category);
            p.writeInt(blockPosition.x());
            p.writeInt(blockPosition.y());
            p.writeInt(blockPosition.z());
            p.writeFloat(1);
            p.writeFloat(1);
        });
    }

    private void handleEntityAction(MinecraftInputStream packet) throws IOException {
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

    private void handleAnimation(MinecraftInputStream packet) throws IOException {
        var hand = packet.readVarInt();
        otherPlayers(player -> player.sendEntityAnimation(this.id, 0));
    }

    public void sendPlayerEntityMetadata(Player player) {
        sendPacket(0x44, p -> {
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

    public void sendEntityMetadata(Entity entity, int index, int type, Object value) {
        sendPacket(0x44, p -> {
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
                case 6 -> ((Slot) value).encode(p);
                case 7 -> p.write(((Boolean) value) ? 1 : 0);
                case 9 -> {
                    if (value instanceof EntityPosition entityPosition) {
                        p.writeLong(entityPosition.blockPosition().encode());
                    } else if (value instanceof BlockPosition blockPosition) {
                        p.writeLong(blockPosition.encode());
                    } else {
                        throw new IllegalStateException("Illegal EntityPosition");
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

    private void handleSteerVehicle(MinecraftInputStream packet) throws IOException {
        float sideways = packet.readFloat();
        float forward = packet.readFloat();
        int flags = packet.read();

        var vehicle = Main.entityById(vehicleEntityId);
        if (vehicle.isEmpty()) {
            return;
        }

        if ((flags & 0x02) != 0) {
            if (vehicle.get() instanceof BoatEntity boat) {
                boat.dismount(this);
                entityPosition.moveBy(0, 1, 0);
                teleport();
            }
        }
    }

    // block place & remove

    public void sendBlockChange(BlockPosition blockPosition, int blockId) {
        sendPacket(0xB, p -> {
            p.writeLong(blockPosition.encode());
            p.writeVarInt(blockId);
        });
    }

    public void sendBlockBreakParticles(BlockPosition blockPosition, short blockId) {
        sendPacket(0x21, p -> {
            p.writeInt(2001);
            p.writePosition(blockPosition);
            p.writeInt(blockId);
            p.writeBoolean(false);
        });
    }

    private void handlePlayerAction(MinecraftInputStream packet) throws IOException {
        var status = packet.readVarInt();
        var location = packet.readLocation();
        var face = packet.read();
        printf("Dig %s : %d%n", location, status);
        switch (status) {
            case 0 -> {
                // "start" digging, which of course means do all of digging
                // for players in creative mode, which is all of them for now.
                var blockId = Main.world.block(location);
                if (selectedItem().itemId() == 586) {
                    sendBlockChange(location, blockId);
                    setEditorLocation(0, location);
                    return;
                }
                Main.world.setBlock(location, 0);
                otherPlayers(player -> {
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
                if (selectedItem().empty()) {
                    return;
                }
                Slot drop = selectedItem().one();
                Slot rest = selectedItem().decrement();
                ItemEntity item = new ItemEntity(eyes(), Velocity.directionMagnitude(entityPosition, 10f), drop);
                item.spawn();
                setSelectedItem(rest);
            }
            case 5 -> {
                // finish interacting
                isShielding = false;
                Main.forEachPlayer(player -> {
                    player.sendPlayerEntityMetadata(this);
                });
                if (isHoldingBow(0)) {
                    ArrowEntity arrow = new ArrowEntity(this, eyes(), useTime());
                    arrow.spawn();
                    arrow.explode = boom;
                    arrow.bulletTime = bulletTime;
                }
            }
            case 6 -> {
                var held = selectedItem();
                inventory.put((short) (selectedHotbarSlot + 36), offhandItem());
                inventory.put((short) 45, held);
                Main.forEachPlayer(player -> {
                    // explicitly including this player, this updates the client
                    player.sendEquipment(this);
                });
            }
        }
    }

    private void handlePlayerPlaceBlock(MinecraftInputStream packet) throws IOException {
        var hand = packet.readVarInt();
        var originalLocation = packet.readLocation();
        if (selectedItem().itemId() == 586 && hand == 0) {
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
            EntityPosition target = new EntityPosition(originalLocation);
            target.x += cursorX;
            target.y += cursorY;
            target.z += cursorZ;
            FireworkEntity firework = new FireworkEntity(target, new Velocity(0, 0, 0));
            firework.spawn();
            return;
        }

        if (isHoldingBoat(hand)) {
            EntityPosition target = new EntityPosition(originalLocation);
            target.x += cursorX;
            target.y += cursorY;
            target.z += cursorZ;
            target.yaw = entityPosition.yaw;
            BoatEntity boat = new BoatEntity(target);
            boat.spawn();
            return;
        }

        if (placeFalling) {
            System.out.println("Spawn falling block");
            final int sandBlock = Registry.itemToBlockDefault(selectedItem().itemId());
            final EntityPosition target = EntityPosition.middle(location);
            FallingBlockEntity block = new FallingBlockEntity(target, sandBlock);
            sendBlockChange(location, current);
            block.spawn();
            return;
        }

        printf("Place %d %s %s %f %f %f %b%n", hand, location, face, cursorX, cursorY, cursorZ, insideBlock);

        AtomicBoolean blockPlacement = new AtomicBoolean(false);
        Main.forEachPlayer(player -> {
            if (player.intersectsLocation(location)) {
                blockPlacement.set(true);
            }
        });
        if (blockPlacement.get()) {
            sendBlockChange(location, current);
            return;
        }

        Slot item = selectedItem();
        Integer blockId = Registry.itemToBlockDefault(item.itemId());
        if (blockId == null) {
            printf("Attempt to place %d is invalid%n", item.itemId());
            sendBlockChange(location, Main.world.block(location));
            return;
        }
        Main.world.setBlock(location, blockId);
        Main.forEachPlayer(player -> player.sendBlockChange(location, blockId));
    }

    boolean isHoldingItem(int item, int hand) {
        if (hand == 0) {
            return selectedItem().itemId() == item;
        } else {
            return offhandItem().itemId() == item;
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

    private void handlePlayerUseItem(MinecraftInputStream packet) throws IOException {
        int hand = packet.readVarInt();
        printf("Use %d %s%n", hand, selectedItem());

        if (isHoldingShield(hand)) {
            isShielding = true;
            Main.forEachPlayer(player -> {
                player.sendPlayerEntityMetadata(this);
            });
        }

        if (isHoldingFirework(hand)) {
            FireworkEntity firework = new FireworkEntity(entityPosition, Velocity.zero());
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

    private void handleCreativeInventoryAction(MinecraftInputStream packet) throws IOException {
        var slotNumber = packet.readShort();
        var slot = Slot.from(packet);
        inventory.put(slotNumber, slot);
        printf("Inventory %d = %s%n", slotNumber, slot);
        if (slotNumber == selectedHotbarSlot + 36 ||
                slotNumber == 45 ||
                slotNumber >= 5 && slotNumber <= 8
        ) {
            otherPlayers(player -> player.sendEquipment(this));
        }
    }

    private void handleHeldItemChange(MinecraftInputStream packet) throws IOException {
        selectedHotbarSlot = packet.readShort();
        printf("Select %d %s%n", selectedHotbarSlot, selectedItem());
        otherPlayers(player -> player.sendEquipment(this));
    }

    public Slot selectedItem() {
        return inventory.get(selectedHotbarSlot + 36);
    }

    public void setSelectedItem(Slot value) {
        inventory.put((short) (selectedHotbarSlot + 36), value);
    }

    Slot clearSelectedItem() {
        return inventory.slots.remove(selectedHotbarSlot + 36);
    }

    Slot offhandItem() {
        return inventory.get(45);
    }

    public void sendHeldItemChange(byte slot) {
        sendPacket(0x3F, p -> {
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
        if (editorBlockPositions[0] == null || editorBlockPositions[1] == null) {
            return 0;
        }
        return ((long) Math.abs(editorBlockPositions[0].x() - editorBlockPositions[1].x()) + 1) *
                ((long) Math.abs(editorBlockPositions[0].y() - editorBlockPositions[1].y()) + 1) *
                ((long) Math.abs(editorBlockPositions[0].z() - editorBlockPositions[1].z()) + 1);
    }

    public void setEditorLocation(int n, BlockPosition blockPosition) {
        editorBlockPositions[n] = blockPosition;
        String message = String.format("EntityPosition %d set to ", n + 1);
        var volume = selectionVolume();
        if (volume == 0) {
            sendEditorNotification(String.format("%s %s", message, editorBlockPositions[n]));
        } else {
            sendEditorNotification(String.format(
                    "%s %s (%d block%s)",
                    message,
                    editorBlockPositions[n],
                    volume,
                    volume == 1 ? "" : "s"
            ));
        }
        sendCUIEvent(n, blockPosition, selectionVolume());
    }

    public void unsetEditorSelection() {
        editorBlockPositions[0] = null;
        editorBlockPositions[1] = null;
        sendCUIEvent(-1, null, 0);
        sendEditorNotification("Selection cleared");
    }

    public void sendCUIEvent(int selection, BlockPosition blockPosition, long volume) {
        if (selection == -1) {
            sendPluginMessage("worldedit:cui", p -> {
                p.writeBytes("s|cuboid");
            });
        } else {
            sendPluginMessage("worldedit:cui", p -> {
                p.writeBytes(
                        String.format(
                                "p|%d|%d|%d|%d|%d",
                                selection,
                                blockPosition.x(),
                                blockPosition.y(),
                                blockPosition.z(),
                                volume
                        )
                );
            });
        }
    }

    // persistence

    void loadFromDb() {
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
                entityPosition = new EntityPosition();
                var xTmp = results.getDouble(3);
                if (!results.wasNull()) {
                    entityPosition.x = xTmp;
                    entityPosition.y = results.getDouble(4);
                    entityPosition.z = results.getDouble(5);
                    entityPosition.pitch = results.getFloat(6);
                    entityPosition.yaw = results.getFloat(7);
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
            statement.setDouble(2, entityPosition.x);
            statement.setDouble(3, entityPosition.y);
            statement.setDouble(4, entityPosition.z);
            statement.setDouble(5, entityPosition.pitch);
            statement.setDouble(6, entityPosition.yaw);
            statement.setInt(7, playerId);
            statement.execute();
            connection.commit();
        }
        inventory.save(playerId);
    }

    // change game state

    public void sendChangeGameState(int reason, float value) {
        sendPacket(0x1D, p -> {
            p.writeByte((byte) reason);
            p.writeFloat(value);
        });
    }

    public void sendChangeGamemode(Player player, int mode) {
        sendPacket(0x32, p -> {
            p.writeVarInt(1);
            p.writeVarInt(1);
            p.writeUUID(player.uuid);
            p.writeVarInt(mode);
        });
    }

    public void changeGamemode(int mode) {
        sendChangeGameState(3, mode);
        Main.forEachPlayer(player -> {
            player.sendChangeGamemode(this, mode);
        });
    }

    // spawn entity

    public void sendSpawnEntity(ObjectEntity entity) {
        sendPacket(0, p -> {
            p.writeVarInt(entity.id);
            p.writeUUID(entity.uuid);
            p.writeVarInt(entity.type());
            p.writePosition(entity.entityPosition);
            p.writeInt(entity.spawnData());
            entity.velocity.write(p);
        });
    }

    public void sendDestroyEntity(Entity entity) {
        sendPacket(0x36, p -> {
            p.writeVarInt(1); // count
            p.writeVarInt(entity.id);
        });
    }

    // equipment

    public void sendEquipment(Player player) {
        sendPacket(0x47, p -> {
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
        if (entityPosition.pitch > 45) {
            return Direction.Down;
        } else if (entityPosition.pitch < -45) {
            return Direction.Up;
        }
        var yaw = (entityPosition.yaw + 45.0) % 360.0;
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

    private void handleInteractEntity(MinecraftInputStream packet) throws IOException {
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

    public void sendSetPassengers(Entity entity, Collection<Entity> passengers) {
        sendPacket(0x4B, p -> {
            p.writeVarInt(entity.id);
            p.writeVarInt(passengers.size());
            for (Entity passenger : passengers) {
                p.writeVarInt(passenger.id);
            }
        });
    }

    private void handleVehicleMove(MinecraftInputStream packet) throws IOException {
        EntityPosition entityPosition = packet.readPosition();
        this.entityPosition = entityPosition;
        var vehicle = Main.entityById(vehicleEntityId);
        if (vehicle.isEmpty()) {
            return;
        }
        vehicle.get().entityPosition = entityPosition;
        otherPlayers(player -> player.sendEntityTeleport(vehicleEntityId, entityPosition));
    }

    private void handleSteerBoat(MinecraftInputStream packet) throws IOException {
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

    public void sendGameOver(Player killer) {
        var chat = new JSONObject();
        chat.put("text", "Oof");
        sendPacket(0x31, p -> {
            p.writeVarInt(2);
            p.writeVarInt(id);
            p.writeInt(killer.id);
            p.writeString(chat.toString());
        });
    }

    private void handleClientStatus(MinecraftInputStream packet) throws IOException {
        var action = packet.readVarInt();
        if (action == 0) { // perform respawn
        }
    }

    private void handleIsFlying(MinecraftInputStream packet) throws IOException {
        var status = packet.read();
        isCreativeFlying = (status & 0x02) != 0;
        isElytraFlying = false;
        Main.forEachPlayer(player -> player.sendPlayerEntityMetadata(this));
    }

    public void sendExplosion(EntityPosition entityPosition, float strength, Collection<BlockPosition> boomBlocks, Velocity playerKick) {
        sendPacket(0x1B, p -> {
            p.writeFloat((float) entityPosition.x);
            p.writeFloat((float) entityPosition.y);
            p.writeFloat((float) entityPosition.z);
            p.writeFloat(strength);
            p.writeInt(boomBlocks.size());
            BlockPosition center = entityPosition.blockPosition();
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
