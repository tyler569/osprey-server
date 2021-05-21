package io.philbrick.minecraft;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class Player {
    enum State {
        Status,
        Login,
        Play,
    }

    Socket connection;
    InputStream instream;
    OutputStream outstream;

    Thread thread;
    State state;

    boolean protocolEncryptionEnabled;

    // Inventory
    // World
    // Vector3 position
    // Orientation

    static final byte[] encryptionToken = "Hello World".getBytes();
    boolean printUnknownPackets = true;

    Player(Socket sock) {
        connection = sock;
        state = State.Status;
        thread = new Thread(this::connectionWrapper);
        thread.start();
    }

    void connectionWrapper() {
        try {
            handleConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void handleConnection() throws IOException {
        instream = new BufferedInputStream(connection.getInputStream());
        outstream = new BufferedOutputStream(connection.getOutputStream());

        while (!connection.isClosed()) {
            handlePacket();
        }
        System.out.println("Leaving handleConnection");
    }

    void handlePacket() throws IOException {
        int packetLen = VarInt.read(instream);
        int packetType = VarInt.read(instream);
        // System.out.format("-> { %d, %d }%n", packetType, packetLen);
        switch (state) {
            case Status -> handleStatusPacket(packetLen, packetType);
            case Login -> handleLoginPacket(packetLen, packetType);
            case Play -> handlePlayPacket(packetLen, packetType);
        }
    }

    void unknownPacket(int len, int type) throws IOException {
        // The "- 1" here is because the packet format is to send the length of
        // *everything* after the length in the first VarInt, including the
        // length of the packet id. We've already read the packet ID at this
        // point, so we can't read it again. The ID is a VarInt, so it can be
        // variable length, but for now all packets are less than 0x80, so
        // in practice today they're all one byte. This will need to change if
        // that ever changes.
        System.out.format("Unknown packet type %d in state %s%n", type, state);
        if (printUnknownPackets) {
            var data = instream.readNBytes(len - 1);
            System.out.print("  ");
            System.out.println(Arrays.toString(data));
        } else {
            instream.skipNBytes(len - 1);
        }
    }

        void handleStatusPacket(int len, int type) throws IOException {
        switch (type) {
            case 0 -> handleHandshake(len);
            case 1 -> handlePing();
            default -> unknownPacket(len, type);
        }
    }

    void handleLoginPacket(int len, int type) throws IOException {
        switch (type) {
            case 0 -> handleLoginStart();
            case 1 -> handleEncryptionResponse();
            default -> unknownPacket(len, type);
        }
    }

    void handlePlayPacket(int len, int type) throws IOException {
        switch (type) {
            default -> unknownPacket(len, type);
        }
    }


    void sendPacket(int type, PacketBuilder closure) throws IOException {
        var m = new ByteArrayOutputStream();
        Protocol.writeVarInt(m, type);
        closure.apply(m);
        VarInt.write(m.size(), outstream);
        var data = m.toByteArray();
        System.out.print("<- ");
        System.out.println(Arrays.toString(data));
        outstream.write(data);
        outstream.flush();
    }

    void writeHandshakeResponse() throws IOException {
        sendPacket(0, (m) -> {
            VarInt.write(Main.handshake_json.length(), m);
            m.write(Main.handshake_json.getBytes());
        });
    }

    void writePingResponse(long number) throws IOException {
        sendPacket(1, (m) -> {
            var b = ByteBuffer.allocate(Long.BYTES);
            b.order(ByteOrder.BIG_ENDIAN);
            b.putLong(number);
            m.write(b.array());
        });
    }

    // handle Status packets

    void handleHandshake(int len) throws IOException {
        if (len > 2) {
            int protocolVersion = VarInt.read(instream);
            String address = Protocol.readString(instream);
            short port = Protocol.readShort(instream);
            int next = VarInt.read(instream);

            System.out.format("handshake: version: %d address: '%s' port: %d next: %d%n",
                protocolVersion, address, port, next);
            if (next == 2) {
                state = State.Login;
            }
        } else {
            writeHandshakeResponse();
        }
    }

    void handlePing() throws IOException {
        long number = Protocol.readLong(instream);
        writePingResponse(number);
    }

    // handle login packets

    void handleLoginStart() throws IOException {
        String name = Protocol.readString(instream);
        System.out.format("login: '%s'%n", name);
        sendPacket(1, (m) -> { // encryption request
            Protocol.writeString(m, "server id not short");
            var encodedKey = Main.encryptionKey.getPublic().getEncoded();
            Protocol.writeVarInt(m, encodedKey.length);
            Protocol.writeBytes(m, encodedKey);
            Protocol.writeVarInt(m, encryptionToken.length);
            Protocol.writeBytes(m, encryptionToken);
        });
    }

    // TODO: break this up
    void handleEncryptionResponse() throws IOException {
        int secretLength = VarInt.read(instream);
        byte[] secret = instream.readNBytes(secretLength);
        int tokenLength = VarInt.read(instream);
        byte[] token = instream.readNBytes(tokenLength);

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
            var protocolKey = new SecretKeySpec(decryptedSecret, "AES");
            var protocolIv = new IvParameterSpec(decryptedSecret);
            var protocolCipherOut = Cipher.getInstance("AES/CFB8/NoPadding");
            protocolCipherOut.init(Cipher.ENCRYPT_MODE, protocolKey, protocolIv);
            var protocolCipherIn = Cipher.getInstance("AES/CFB8/NoPadding");
            protocolCipherIn.init(Cipher.DECRYPT_MODE, protocolKey, protocolIv);

            instream = new CipherInputStream(instream, protocolCipherIn);
            outstream = new CipherOutputStream(outstream, protocolCipherOut);
            protocolEncryptionEnabled = true;
        } catch (Exception e) {
            e.printStackTrace();
            // login failure
            return;
        }

        System.out.println("Writing Login Success!");
        sendPacket(2, (m) -> { // login success
            Protocol.writeLong(m, 0);
            Protocol.writeLong(m, 0);
            Protocol.writeString(m, "tyler569");
        });

        // login success
        state = State.Play;

        sendPacket(0x24, (m) -> { // Join Game
            Protocol.writeInt(m, 1); // Entity ID
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

    // handle play packets
}
