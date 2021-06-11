package com.pygostylia.osprey;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Registry {
    static JSONObject blockStates;
    static JSONObject registries;
    static Map<Integer, Integer> itemToBlock = new HashMap<>();
    static Map<Integer, String> itemNames = new HashMap<>();
    static Map<Integer, String> entityNames = new HashMap<>();

    public static void setup(String directory) throws IOException {
        blockStates = new JSONObject(Files.readString(Path.of(directory, "reports", "blocks.json")));
        registries = new JSONObject(Files.readString(Path.of(directory, "reports", "registries.json")));
        populateItemInfo();
        populateEntityInfo();
    }

    public static Integer blockDefaultId(String name) {
        var blockEntry = blockStates.optJSONObject(name);
        if (blockEntry == null) {
            return null;
        }
        var blockStates = blockEntry.getJSONArray("states");

        for (var maybeState : blockStates) {
            if (maybeState instanceof JSONObject state) {
                if (state.optBoolean("default", false)) {
                    return state.getInt("id");
                }
            }
        }
        return null;
    }

    private static void populateItemInfo() {
        var items = registries.getJSONObject("minecraft:item").getJSONObject("entries");
        Iterator<String> itemsKeys = items.keys();

        while (itemsKeys.hasNext()) {
            String key = itemsKeys.next();
            var itemId = items.getJSONObject(key).getInt("protocol_id");
            itemNames.put(itemId, key);
            var blockId = blockDefaultId(key);
            if (blockId != null) {
                itemToBlock.put(itemId, blockId);
            }
        }
    }

    private static void populateEntityInfo() {
        var entities = registries.getJSONObject("minecraft:entity_type").getJSONObject("entities");
        Iterator<String> entitiesKeys = entities.keys();

        while (entitiesKeys.hasNext()) {
            String key = entitiesKeys.next();
            var entityId = entities.getJSONObject(key).getInt("protocol_id");
            entityNames.put(entityId, key);
        }
    }

    public static Integer lookupId(String registry, String name) {
        var items = registries.getJSONObject(registry).getJSONObject("entries");
        var item = items.optJSONObject(name);
        if (item == null) {
            return null;
        }
        return item.getInt("protocol_id");
    }

    public static Integer item(String name) {
        return lookupId("minecraft:item", name);
    }

    public static Integer entity(String name) {
        return lookupId("minecraft:entity_type", name);
    }

    public static Integer blockType(String name) {
        return lookupId("minecraft:block", name);
    }

    public static Integer itemToBlockDefault(int item) {
        return itemToBlock.get(item);
    }

    public static Integer itemToBlockDefault(String itemName) {
        var item = item(itemName);
        if (item == null) return null;
        return itemToBlock.get(item);
    }

    public static String itemName(int itemId) {
        return itemNames.get(itemId);
    }

    public static String entityName(int entityId) {
        return entityNames.get(entityId);
    }
}
