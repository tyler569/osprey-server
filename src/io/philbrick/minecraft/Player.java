package io.philbrick.minecraft;

import io.philbrick.minecraft.nbt.*;
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

    String name;
    Connection connection;
    Duration ping;
    Thread thread;
    State state;
    int entityId;
    UUID uuid;
    ArrayList<Player> nearbyPlayers;
    Position position;

    // Inventory
    // World
    // Vector3 position
    // Orientation

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
        }
    }

    void disconnect() {
        if (connection.isEstablished()) {
            for (var player : Main.players) {
                try {
                    player.sendDespawnPlayer(this);
                    player.sendRemovePlayer(this);
                    player.sendSystemMessage(String.format("%s has left the game.", name));
                } catch (IOException ignored) {
                }
            }
        }
        Main.reaper.appendDeadThread(thread);
        Main.players.remove(this);
    }

    void teleport(Location location) throws IOException {
        sendPositionLook(location.x(), location.y(), location.z(), 0, 0);
        position.x = location.x();
        position.y = location.y();
        position.z = location.z();
        for (var player : Main.players) {
        }
    }

    void handleConnection() throws IOException {
        while (true) {
            if (connection.isClosed()) break;
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
        System.out.format("Unknown packet type %d in state %s%n", packet.type, state);
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
                uuid = UUID.randomUUID();
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
        String name = packet.readString();
        this.name = name;
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

        sendAddPlayers(Main.players);
        sendAddPlayer(this);
        position = new Position();
        for (var player : Main.players) {
            player.sendAddPlayer(this);
            player.sendSpawnPlayer(this);
            sendSpawnPlayer(player);
            player.sendSystemMessage(String.format("%s has joined the game.", name));
        }
        Main.players.add(this);
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
        sendJoinGame();
        sendBrand("corvidio");
    }

    // handle play packets

    void sendChunk(int x, int z) throws IOException {
        var heightMap = new Long[37];
        Arrays.fill(heightMap, 0x0100804020100804L);
        heightMap[36] = 0x0000000020100804L;
        var heightmapNBT = new NBTCompound(null,
            new NBTLongArray("MOTION_BLOCKING",
                heightMap
            )
        );
        var chunkData = new ByteArrayOutputStream();
        for (int y = 0; y < 16; y++) {
            if (y == 0) { // block count
                Protocol.writeShort(chunkData, 256);
            } else {
                Protocol.writeShort(chunkData, 0);
            }
            Protocol.writeByte(chunkData, 4); // bits per block
            Protocol.writeVarInt(chunkData, 2); // palette length
            Protocol.writeVarInt(chunkData, 0); // 0 : air
            Protocol.writeVarInt(chunkData, 1); // 1 : stone
            Protocol.writeVarInt(chunkData, 16 * 16);
            for (int by = 0; by < 16; by++) {
                for (int bz = 0; bz < 16; bz++) {
                    // for (int bx = 0; bx < 16; bx++) {
                    // }
                    if (by == 0 && y == 0) {
                        Protocol.writeLong(chunkData, 0x1111_1111_1111_1111L);
                    } else {
                        Protocol.writeLong(chunkData, 0);
                    }
                }
            }
        }

        connection.sendPacket(0x20, (m) -> {
            Protocol.writeInt(m, x);
            Protocol.writeInt(m, z);
            Protocol.writeBoolean(m, true);
            Protocol.writeVarInt(m, 0xFFFF); // primary bitmask
            heightmapNBT.encode(m);
            Protocol.writeVarInt(m, 1024);
            for (int i = 0; i < 1024; i++) {
                Protocol.writeVarInt(m, 0);
            }
            Protocol.writeVarInt(m, chunkData.size());
            Protocol.writeBytes(m, chunkData.toByteArray());
            Protocol.writeVarInt(m, 0);
            // Array of NBT containing block entities
        });
    }

    void sendPositionLook(double x, double y, double z, float yaw, float pitch) throws IOException {
        connection.sendPacket(0x34, (m) -> {
            Protocol.writeDouble(m, x);
            Protocol.writeDouble(m, y);
            Protocol.writeDouble(m, z);
            Protocol.writeFloat(m, yaw);
            Protocol.writeFloat(m, pitch);
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

    void sendSpawnPosition() throws IOException {
        connection.sendPacket(0x42, (m) -> {
            Protocol.writePosition(m, 0, 32, 0);
        });
    }

    void handleSettings(Packet packet) throws IOException {
        sendBrand("corvidio");
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                sendChunk(x, z);
            }
        }
        sendSpawnPosition();
        sendUpdateChunkPosition(0, 0);
        sendPositionLook(0, 32, 0, 0, 0);
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

    void sendSystemMessage(String message) throws IOException {
        var chat = new JSONObject();
        chat.put("text", message);
        chat.put("color", "yellow");
        connection.sendPacket(0x0E, (m) -> {
            Protocol.writeString(m, chat.toString());
            Protocol.writeByte(m, 0);
            Protocol.writeLong(m, 0);
            Protocol.writeLong(m, 0);
        });
    }

    void handleChat(Packet packet) throws IOException {
        String message = packet.readString();
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

    void updatePosition(double x, double y, double z, float pitch, float yaw, boolean onGround) throws IOException {
        double delta_x = x - position.x;
        double delta_y = y - position.y;
        double delta_z = z - position.z;
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
}