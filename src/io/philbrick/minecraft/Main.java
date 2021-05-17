package io.philbrick.minecraft;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class Main {
    static byte[] readPacket(InputStream is) throws IOException {
        int packetLen = VarInt.read(is);
        var buffer = new byte[packetLen];
        int len = is.read(buffer);
        if (len < packetLen) {
            // Something went wrong tm
        }
        return buffer;
    }

    static final String json = """
        {
          "version": {
            "name": "1.16.5",
            "protocol": 754
          },
          "players": {
            "max": 1000,
            "online": 1,
            "sample": [
              {
                "name": "tyler569",
                "id": "492c1496-f590-4a6f-a899-37f9b87a8238"
              }
            ]
          },
          "description": {
            "text": "Hello World"
          }
        }
        """;

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
            VarInt.write(json.length(), m);
            m.write(json.getBytes());
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

    static void joinWrapper(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void connectionHandler(Socket connection) {
        try {
            var instream = connection.getInputStream();
            var outstream = connection.getOutputStream();

            while (!connection.isClosed()) {
                byte[] pkt = readPacket(instream);
                System.out.print("-> ");
                System.out.println(Arrays.toString(pkt));
                if (pkt[0] == 0 && pkt.length == 1) {
                    writeHandshakeResponse(outstream);
                }
                if (pkt[0] == 1) {
                    var buffer = ByteBuffer.wrap(Arrays.copyOfRange(pkt, 1, 9));
                    buffer.order(ByteOrder.BIG_ENDIAN);
                    writePingResponse(outstream, buffer.getLong());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        final var socket = new ServerSocket(25565);
        var threads = new ArrayList<Thread>();
        while (!socket.isClosed()) {
            var connection = socket.accept();
            var thread = new Thread(() -> connectionHandler(connection));
            thread.start();
            threads.add(thread);
        }
        threads.forEach((thread) -> joinWrapper(thread));
    }
}
