package io.philbrick.minecraft;

import java.io.*;
import java.net.*;
import java.nio.*;
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static ArrayList<Player> players = new ArrayList<Player>();

    public static void main(String[] args) throws IOException {
        final var socket = new ServerSocket(25565);
        var threads = new ArrayList<Thread>();
        while (!socket.isClosed()) {
            var connection = socket.accept();
            players.add(new Player(connection));
        }
    }
}
