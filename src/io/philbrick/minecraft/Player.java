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

    SecretKey protocolKey;
    IvParameterSpec protocolIv;
    Cipher protocolCipher;
    boolean protocolEncryptionEnabled;

    // Inventory
    // World
    // Vector3 position
    // Orientation

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

    void sendPacket(int type, PacketBuilder closure) throws IOException {
        var m = new ByteArrayOutputStream();
        Protocol.writeVarInt(m, type);
        closure.apply(m);
        VarInt.write(m.size(), outstream);
        var data = m.toByteArray();
        System.out.println(Arrays.toString(data));
        if (false && protocolEncryptionEnabled) {
            var encrypted = protocolCipher.update(data);
            System.out.println(Arrays.toString(encrypted));
            outstream.write(encrypted);
        } else {
            outstream.write(data);
        }
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

    void handleStatusPacket(int len, int type) throws IOException {
        switch (type) {
            case 0: // handshake
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
                break;
            case 1: // ping
                long number = Protocol.readLong(instream);
                writePingResponse(number);
                break;
        }
    }

    void handleLoginPacket(int len, int type) throws IOException {
        switch (type) {
            case 0: // login start
                String name = Protocol.readString(instream);
                System.out.format("login: '%s'%n", name);
                sendPacket(1, (m) -> { // encryption request
                    Protocol.writeString(m, "server id not short");
                    var encodedKey = Main.encryptionKey.getPublic().getEncoded();
                    Protocol.writeVarInt(m, encodedKey.length);
                    Protocol.writeBytes(m, encodedKey);
                    Protocol.writeVarInt(m, 4);
                    Protocol.writeBytes(m, new byte[]{1, 2, 3, 4});
                });
                break;
            case 1: // encryption response
                int secretLength = VarInt.read(instream);
                byte[] secret = instream.readNBytes(secretLength);
                int tokenLength = VarInt.read(instream);
                byte[] token = instream.readNBytes(tokenLength);

                byte[] decryptedSecret;
                byte[] decryptedToken;

                // System.out.format("  secret: %d %s token: %d %s%n",
                //         secretLength, Arrays.toString(secret),
                //         tokenLength, Arrays.toString(token));

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
                    protocolKey = new SecretKeySpec(decryptedSecret, "AES");
                    protocolIv = new IvParameterSpec(decryptedSecret);
                    protocolCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                    protocolCipher.init(Cipher.ENCRYPT_MODE, protocolKey, protocolIv);
                } catch (Exception e) {
                    e.printStackTrace();
                    // login failure
                    return;
                }

                protocolEncryptionEnabled = true;
                instream = new CipherInputStream(instream, protocolCipher);
                outstream = new CipherOutputStream(outstream, protocolCipher);

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
                    Protocol.writeString(m, "minecraft:overworld"); // the world
                    // etc TODO
                });
        }
    }

    void handlePlayPacket(int len, int type) throws IOException {
    }

    void handlePacket() throws IOException {
        int packetLen = VarInt.read(instream);
        int packetType = VarInt.read(instream);
        switch (state) {
            case Status -> handleStatusPacket(packetLen, packetType);
            case Login -> handleLoginPacket(packetLen, packetType);
            case Play -> handlePlayPacket(packetLen, packetType);
        }
    }

    void handleConnection() throws IOException {
        instream = connection.getInputStream();
        outstream = connection.getOutputStream();

        while (!connection.isClosed()) {
            handlePacket();
        }
        System.out.println("Leaving handleConnection");
    }
}
