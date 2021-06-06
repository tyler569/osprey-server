package com.pygostylia.osprey;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Registry {
    JSONObject blockStates;
    JSONObject registries;
    Map<Integer, Integer> itemToBlock = new HashMap<>();

    public Registry(String directory) throws IOException {
        blockStates = new JSONObject(Files.readString(Path.of(directory, "reports", "blocks.json")));
        registries = new JSONObject(Files.readString(Path.of(directory, "reports", "registries.json")));
        populateItemToBlockDefault();
    }

    Integer blockDefaultId(String name) {
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

    private void populateItemToBlockDefault() {
        var items = registries.getJSONObject("minecraft:item").getJSONObject("entries");
        Iterator<String> itemsKeys = items.keys();

        while (itemsKeys.hasNext()) {
            String key = itemsKeys.next();
            var itemId = items.getJSONObject(key).getInt("protocol_id");
            var blockId = blockDefaultId(key);
            if (blockId != null) {
                itemToBlock.put(itemId, blockId);
            }
        }

    }

    public Integer lookupId(String registry, String name) {
        var items = registries.getJSONObject(registry).getJSONObject("entries");
        var item = items.optJSONObject(name);
        if (item == null) {
            return null;
        }
        return item.getInt("protocol_id");
    }

    public Integer item(String name) {
        return lookupId("minecraft:item", name);
    }

    public Integer entity(String name) {
        return lookupId("minecraft:entity_type", name);
    }

    public Integer blockType(String name) {
        return lookupId("minecraft:block", name);
    }

    public Integer itemToBlockDefault(int item) {
        return itemToBlock.get(item);
    }

    public Integer itemToBlockDefault(String itemName) {
        var item = item(itemName);
        if (item == null) return null;
        return itemToBlock.get(item);
    }
}
