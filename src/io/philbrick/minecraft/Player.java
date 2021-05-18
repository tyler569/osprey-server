package io.philbrick.minecraft;

import java.io.*;
import java.net.*;
import java.nio.*;

public class Player {
    enum State {
        Status,
        Login,
        Play,
    }

    Socket connection;
    Thread thread;
    State state;

    InputStream instream;
    OutputStream outstream;

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

    static void sendPacket(OutputStream os, PacketBuilder closure) throws IOException {
        var m = new ByteArrayOutputStream();
        closure.apply(m);
        VarInt.write(m.size(), os);
        os.write(m.toByteArray());
        os.flush();
    }

    static void writeHandshakeResponse(OutputStream os) throws IOException {
        sendPacket(os, (m) -> {
            m.write((byte)0);
            VarInt.write(Main.handshake_json.length(), m);
            m.write(Main.handshake_json.getBytes());
        });
    }

    static void writePingResponse(OutputStream os, long number) throws IOException {
        sendPacket(os, (m) -> {
            var b = ByteBuffer.allocate(Long.BYTES);
            b.order(ByteOrder.BIG_ENDIAN);
            b.putLong(number);
            m.write((byte)1);
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
                break;
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
