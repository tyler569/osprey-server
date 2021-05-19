package io.philbrick.minecraft;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class Main {
    static final String handshake_json = """
        {
          "version": {
            "name": "1.16.5",
            "protocol": 754
          },
          "players": {
            "max": -1,
            "online": 1,
            "sample": [
              {
                "name": "tyler569",
                "id": "492c1496-f590-4a6f-a899-37f9b87a8238"
              }
            ]
          },
          "description": {
            "text": "Hello World!"
          }
        }
        """;

    static KeyPair encryptionKey;
    static ArrayList<Player> players = new ArrayList<>();
    // static ArrayList<World> worlds = new ArrayList<>();
    // etc

    public static void main(String[] args) throws IOException {
        try {
            var kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            encryptionKey = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        final var socket = new ServerSocket(25565);
        while (!socket.isClosed()) {
            var connection = socket.accept();
            players.add(new Player(connection));
        }
    }
}
