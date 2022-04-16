package com.pygostylia.osprey;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class World {
    String databaseURL;
    HashMap<ChunkPosition, Chunk> loadedChunks;

    private World() {
        loadedChunks = new HashMap<>();
    }

    Connection connect() throws SQLException {
        var connection = DriverManager.getConnection(databaseURL);
        connection.setAutoCommit(false);
        return connection;
    }

    static World open(String filename) throws SQLException {
        var world = new World();
        world.databaseURL = String.format("jdbc:sqlite:%s", filename);
        world.migrate();
        return world;
    }

    Chunk load(ChunkPosition position) {
        var chunk = loadedChunks.get(position);
        if (chunk == null) {
            try {
                chunk = loadFromDisk(position);
                loadedChunks.put(position, chunk);
            } catch (SQLException e) {
                System.out.printf("Failed to load %s from disk%n", position);
                System.out.println(e.getMessage());
                e.printStackTrace(System.out);
            }
        }
        if (chunk == null) {
            chunk = Chunk.defaultGeneration();
            loadedChunks.put(position, chunk);
        }
        return chunk;
    }

    Chunk load(int x, int z) throws IOException {
        return load(new ChunkPosition(x, z));
    }

    void saveChunk(ChunkPosition position) throws SQLException, IOException {
        if (!loadedChunks.containsKey(position)) {
            return;
        }
        var chunk = loadedChunks.get(position);
        var chunkBlob = chunk.encodeBlob();
        String sql = """
                INSERT OR REPLACE INTO chunks (x, z, data)
                VALUES (?, ?, ?);
                """;
        try (var connection = connect(); var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, position.getX());
            statement.setInt(2, position.getZ());
            statement.setBytes(3, chunkBlob);
            statement.execute();
            connection.commit();
        }
    }

    void saveChunkSafe(ChunkPosition position) {
        try {
            saveChunk(position);
        } catch (Exception e) {
            System.out.printf("Failed to save %s%n", position);
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    Chunk loadFromDisk(ChunkPosition position) throws SQLException {
        String sql = """
                SELECT data FROM chunks
                WHERE x = ? AND z = ?;
                """;
        try (var connection = connect(); var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, position.getX());
            statement.setInt(2, position.getZ());
            var results = statement.executeQuery();
            if (results.isClosed()) return null;
            byte[] blob = results.getBytes(1);
            if (blob != null && blob.length != 0) {
                return Chunk.fromBlob(blob);
            } else {
                return null;
            }
        }
    }

    private void migrationStep(int id, String... sqls) throws SQLException {
        try (var connection = connect()) {
            PreparedStatement statement;
            for (var sql : sqls) {
                statement = connection.prepareStatement(sql);
                statement.execute();
            }
            statement = connection.prepareStatement("""
                    INSERT INTO schema_migrations VALUES (?);
                    """);
            statement.setInt(1, id);
            statement.execute();
            connection.commit();
        }
    }

    private void migrate() throws SQLException {
        int version;

        String sql = """
                CREATE TABLE IF NOT EXISTS schema_migrations(
                    id INTEGER NOT NULL PRIMARY KEY
                );
                """;
        try (var connection = connect(); var statement = connection.prepareStatement(sql)) {
            statement.execute();
            connection.commit();
        }

        sql = """
                SELECT MAX(id) FROM schema_migrations;
                """;
        try (var connection = connect(); var statement = connection.prepareStatement(sql)) {
            var results = statement.executeQuery();
            version = results.getInt(1);
            System.out.printf("Found world version %d%n", version);
        }

        switch (version) {
            case 0:
                migrationStep(1, """
                        CREATE TABLE chunks (
                            x INTEGER NOT NULL,
                            z INTEGER NOT NULL,
                            data BLOB,
                            PRIMARY KEY(x, z)
                        );
                        """);
            case 1:
                migrationStep(2, """
                        CREATE TABLE players (
                            id INTEGER NOT NULL PRIMARY KEY,
                            uuid STRING,
                            name STRING
                        );
                        """, """
                        CREATE TABLE inventory_slots (
                            id INTEGER NOT NULL PRIMARY KEY,
                            player_id INTEGER NOT NULL,
                            slot_number INTEGER NOT NULL,
                            item_id INTEGER NOT NULL,
                            stack_count INTEGER NOT NULL,
                            extra_data BLOB,
                            FOREIGN KEY(player_id) REFERENCES players(id)
                        );
                        """);
            case 2:
                migrationStep(3, """
                        ALTER TABLE players
                        ADD COLUMN selected_slot INTEGER;
                        """);
            case 3:
                migrationStep(4,
                        "ALTER TABLE players ADD COLUMN x REAL;",
                        "ALTER TABLE players ADD COLUMN y REAL;",
                        "ALTER TABLE players ADD COLUMN z REAL;",
                        "ALTER TABLE players ADD COLUMN pitch REAL;",
                        "ALTER TABLE players ADD COLUMN yaw REAL;"
                );

            default:
        }
    }

    public void save() {
        for (ChunkPosition position : loadedChunks.keySet()) {
            if (loadedChunks.get(position).modified) {
                saveChunkSafe(position);
                loadedChunks.get(position).modified = false;
            }
        }
    }

    public void setBlock(BlockPosition blockPosition, int blockId) {
        var affectedChunkPosition = blockPosition.chunkPosition();
        var affectedChunk = load(affectedChunkPosition);
        affectedChunk.setBlock(blockPosition.positionInChunk(), blockId);
    }

    public short block(BlockPosition blockPosition) {
        var affectedChunkPosition = blockPosition.chunkPosition();
        var affectedChunk = load(affectedChunkPosition);
        return affectedChunk.block(blockPosition.positionInChunk());
    }
}
