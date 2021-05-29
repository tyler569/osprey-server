package io.philbrick.minecraft;

import java.sql.*;
import java.util.*;

public class Inventory {
    Map<Integer, Slot> slots;

    Inventory() {
        slots = new HashMap<>();
        slots.put(36, new Slot(1));
        slots.put(44, new Slot(586));
    }

    int size() {
        return 45;
    }

    public void put(short slotNumber, Slot slot) {
        slots.put((int)slotNumber, slot);
    }

    public Slot get(int slotNumber) {
        return slots.getOrDefault(slotNumber, new Slot());
    }

    public void save(int playerId) throws SQLException {
        try (var connection = Main.world.connect()) {
            String sql = """
                DELETE FROM inventory_slots
                WHERE player_id = ?;
                """;
            var statement = connection.prepareStatement(sql);
            statement.setInt(1, playerId);
            statement.execute();
            for (var item : slots.entrySet()) {
                if (item.getValue().empty) {
                    continue;
                }
                sql = String.format("""
                    INSERT INTO inventory_slots (player_id, slot_number, item_id, stack_count)
                    VALUES (?, ?, ?, ?);
                    """);
                statement = connection.prepareStatement(sql);
                statement.setInt(1, playerId);
                statement.setInt(2, item.getKey());
                statement.setInt(3, item.getValue().itemId);
                statement.setInt(4, item.getValue().count);
                statement.execute();
            }
            connection.commit();
        }
    }

    public static Inventory loadFromDb(int playerId) throws SQLException {
        Inventory output = new Inventory();
        String sql = """
            SELECT slot_number, item_id, stack_count
            FROM inventory_slots
            WHERE player_id = ?;
            """;
        try (var connection = Main.world.connect();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, playerId);
            var results = statement.executeQuery();
            if (!results.isClosed()) {
                do {
                    Slot s = new Slot();
                    s.itemId = results.getInt(2);
                    s.count = results.getInt(3);
                    s.empty = false;
                    output.put((short) results.getInt(1), s);
                } while (results.next());
            }
        }
        return output;
    }
}