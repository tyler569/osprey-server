package io.philbrick.minecraft;

import javax.crypto.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.*;
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

    byte[] encryptionKey;

    // Inventory
    // World
    // Vector3 position
    // Orientation

    Player(Socket sock) {
        connection = sock;
        state = State.Status;
        thread = new Thread(() -> connectionWrapper());
        thread.start();
    }

    void connectionWrapper() {
        try {
            handleConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void sendPacket(OutputStream os, int type, PacketBuilder closure) throws IOException {
        var m = new ByteArrayOutputStream();
        Protocol.writeVarInt(m, type);
        closure.apply(m);
        VarInt.write(m.size(), os);
        os.write(m.toByteArray());
        os.flush();
    }

    static void writeHandshakeResponse(OutputStream os) throws IOException {
        sendPacket(os, 0, (m) -> {
            VarInt.write(Main.handshake_json.length(), m);
            m.write(Main.handshake_json.getBytes());
        });
    }

    static void writePingResponse(OutputStream os, long number) throws IOException {
        sendPacket(os, 1, (m) -> {
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
                    writeHandshakeResponse(outstream);
                }
                break;
            case 1: // ping
                long number = Protocol.readLong(instream);
                writePingResponse(outstream, number);
                break;
        }
    }

    void handleLoginPacket(int len, int type) throws IOException {
        switch (type) {
            case 0: // login start
                String name = Protocol.readString(instream);
                System.out.format("login: '%s'%n", name);
                sendPacket(outstream, 1, (m) -> { // encryption request
                    Protocol.writeVarInt(m, 1);
                    Protocol.writeString(m, "");
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

                System.out.format("  secret: %d %s token: %d %s%n",
                        secretLength, Arrays.toString(secret),
                        tokenLength, Arrays.toString(token));

                try {
                    var cipher = Cipher.getInstance("RSA");
                    cipher.init(Cipher.DECRYPT_MODE, Main.encryptionKey.getPrivate());
                    var decryptedToken = cipher.doFinal(token);
                    System.out.format("  decrypted token: %s%n",
                            Arrays.toString(decryptedToken));

                    cipher.init(Cipher.DECRYPT_MODE, Main.encryptionKey.getPrivate());
                    encryptionKey = cipher.doFinal(secret);
                    System.out.format("  decrypted secret key: %s%n",
                            Arrays.toString(encryptionKey));
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
            System.out.println("Waiting for a packet");
            handlePacket();
        }
        System.out.println("Leaving handleConnection");
    }
}
