package com.pygostylia.osprey;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockState {
    private long state;

    public BlockState(String blockName, JSONObject registryState) {
        var blockId = blockIds.get(blockName);
        if (blockId == null) {
            throw new IllegalArgumentException("Illegal block name");
        }
        var blockType = blockTypes.get(blockId);
        var fields = typeFields.get(blockType);

        long internal = 0;
        internal |= blockId;
        internal |= (long) blockType.type() << blockIdLength;
        var properties = registryState.optJSONObject("properties");
        if (properties != null) {
            for (var prop : properties.keySet()) {
                var field = fields.get(prop);
                var propValue = properties.getString(prop);
                var propInt = field.values().get(propValue);
                internal |= (long) propInt << field.offset;
            }
        }
        state = internal;
    }

    public BlockState(int blockId) {
        state = internalLongs.get(blockDefaultStates.get(blockId));
    }

    public BlockState(int blockId, HashMap<String, String> states) {
        this(blockId);
        for (var state : states.entrySet()) {
            set(state.getKey(), state.getValue());
        }
    }

    private static final Pattern statePattern = Pattern.compile("([^\\[]+)(?:\\[([^]]*)])?");

    public BlockState(String blockIdString) {
        Matcher m = statePattern.matcher(blockIdString);
        String blockName;
        String props;
        if (m.find()) {
            blockName = m.group(1);
            props = m.group(2);
        } else {
            throw new IllegalArgumentException("Invalid block state pattern");
        }
        var blockId = blockIds.get(blockName);
        if (blockId == null) {
            throw new IllegalArgumentException("Invalid block type");
        }
        state = internalLongs.get(blockDefaultStates.get(blockId));
        if (props != null) {
            for (String prop : props.split(",")) {
                String[] kv = prop.split("=", 2);
                if (kv.length != 2) {
                    throw new IllegalArgumentException("Invalid block prop format");
                }
                boolean success = set(kv[0].toLowerCase(), kv[1].toLowerCase());
                if (!success) {
                    throw new IllegalArgumentException(String.format("Invalid prop '%s' for block '%s'", kv[0], blockName));
                }
            }
        }
    }

    public BlockState(InputStream stream) {
        // consumes one state from the stream
    }

    public BlockState(short protocolId) {
        state = internalLongs.get(protocolId);
    }

    public BlockState(long value) {
        state = value;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        var blockId = blockId();
        sb.append(blockName());
        if (fields() != null) {
            if (fields().size() > 0) {
                sb.append("[");
            }
            boolean first = true;
            for (var field : fields().entrySet()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(field.getKey());
                sb.append("=");
                sb.append(get(field.getKey()));
            }
            if (fields().size() > 0) {
                sb.append("]");
            }
        }
        return sb.toString();
    }

    public String blockName() {
        return blockNames.get(blockId());
    }

    public long longValue() {
        return state;
    }

    public int blockId() {
        return (int) state & 0x3FF;
    }

    public Short protocolId() {
        return protocolIds.get(state);
    }

    public String get(String key) {
        var bitFieldDescription = fields().get(key);
        if (bitFieldDescription == null) {
            return null;
        }
        var value = getRaw(key);
        for (var fieldValue : bitFieldDescription.values.entrySet()) {
            // reversing the HashMap
            if (value.equals(fieldValue.getValue())) {
                return fieldValue.getKey();
            }
        }
        throw new IllegalStateException("Invalid value stored in internal long");
    }

    public boolean set(String key, String value) {
        var bitFieldDescription = fields().get(key);
        if (bitFieldDescription == null) {
            return false;
        }
        int intValue = bitFieldDescription.values.get(value);
        return setRaw(key, intValue);
    }

    public Integer getRaw(String key) {
        var bitFieldDescription = fields().get(key);
        if (bitFieldDescription == null) {
            return null;
        }
        var value = state >> bitFieldDescription.offset;
        value &= bitFieldDescription.maskAtZero();
        return (int) value;
    }

    public boolean setRaw(String key, int value) {
        var bitFieldDescription = fields().get(key);
        if (bitFieldDescription == null) {
            return false;
        }
        if (log2(value) > bitFieldDescription.width) {
            throw new IllegalArgumentException(String.format("Value %d is too large for field '%s'", value, key));
        }
        state &= ~bitFieldDescription.mask();
        state |= (long) value << bitFieldDescription.offset;
        return true;
    }

    private Type type() {
        // or I also have it in the long -- I bet that doesn't matter.
        // since they're never stored, I'll always have this hashmap.
        // potentially a perf difference?
        return blockTypes.get(blockId());
    }

    private Map<String, BitField> fields() {
        return typeFields.get(type());
    }

    // Several of these should be local to the generating code
    private static final Map<String, Integer> blockIds = new HashMap<>();
    private static final Map<Integer, String> blockNames = new HashMap<>();
    private static final Map<Map<String, BitField>, Type> blockStateTypes = new HashMap<>();
    private static final Map<Integer, Type> blockTypes = new HashMap<>();
    private static final Map<Type, Map<String, BitField>> typeFields = new HashMap<>();
    private static final Map<Short, Long> internalLongs = new HashMap<>();
    private static final Map<Long, Short> protocolIds = new HashMap<>();
    private static final Map<Integer, Short> blockDefaultStates = new HashMap<>();

    private static final int blockIdLength = 10;
    private static final int typeIdLength = 8;

    private static record Type(int type) {
    }

    private static record BitField(int offset, int width, Map<String, Integer> values) {
        int maskAtZero() {
            return (1 << width) - 1;
        }

        int mask() {
            return maskAtZero() << offset;
        }
    }

    private static int nextType;

    static int log2(int v) {
        if (v == 0) return 0;
        return 32 - Integer.numberOfLeadingZeros(v - 1);
    }

    private static Map<String, BitField> generatePropFields(JSONObject properties) {
        var result = new HashMap<String, BitField>();
        int offset = blockIdLength + typeIdLength;
        if (properties != null) {
            for (var prop : properties.keySet()) {
                var count = properties.getJSONArray(prop).length();
                var bits = log2(count);
                var values = new HashMap<String, Integer>();
                int valueN = 0;
                for (var valuesObj : properties.getJSONArray(prop)) {
                    String value = (String) valuesObj;
                    values.put(value, valueN++);
                }
                result.put(prop, new BitField(offset, bits, values));
                offset += bits;
                if (offset > 63) {
                    throw new IllegalStateException("Ran out of bits to encode block state");
                }
            }
        }
        return result;
    }

    private static Type findOrCreateType(Map<String, BitField> propFields) {
        if (blockStateTypes.containsKey(propFields)) {
            return blockStateTypes.get(propFields);
        } else {
            var newType = new Type(nextType++);
            blockStateTypes.put(propFields, newType);
            typeFields.put(newType, propFields);
            return newType;
        }
    }

    public static void main(String[] args) throws IOException {
        JSONObject blocks = new JSONObject(Files.readString(Path.of("generated/reports/blocks.json")));
        JSONObject registry = new JSONObject(Files.readString(Path.of("generated/reports/registries.json")));
        JSONObject blockIdsJSON = registry.getJSONObject("minecraft:block").getJSONObject("entries");

        for (var key : blockIdsJSON.keySet()) {
            blockIds.put(key, blockIdsJSON.getJSONObject(key).getInt("protocol_id"));
            blockNames.put(blockIdsJSON.getJSONObject(key).getInt("protocol_id"), key);
        }

        System.out.println(blockIds);
        System.out.println(blockNames);

        for (var key : blocks.keySet()) {
            JSONObject block = blocks.getJSONObject(key);
            final var props = block.optJSONObject("properties");
            final var thisTypeFields = generatePropFields(props);
            final Type thisType = findOrCreateType(thisTypeFields);
            final var blockId = blockIds.get(key);
            blockTypes.put(blockId, thisType);
            var blockType = blockTypes.get(blockId);
            var fields = typeFields.get(blockType);

            JSONArray states = block.optJSONArray("states");
            for (Object map : states) {
                var registryState = (JSONObject) map;
                var blockState = new BlockState(key, registryState);
                short stateId = (short) registryState.getInt("id");
                internalLongs.put(stateId, blockState.state);
                protocolIds.put(blockState.state, stateId);
                if (registryState.optBoolean("default")) {
                    blockDefaultStates.put(blockId, stateId);
                }
            }
        }
        // System.out.println(blockStateTypes);
        // System.out.println(typeFields);
        // System.out.println(blockTypes);
        // System.out.println(internalLongs);

        for (short protocolId = 0; protocolId < 20; protocolId++) {
            var state = new BlockState(protocolId);
            System.out.printf("%s %016x %s%n", state.protocolId(), state.longValue(), state);
        }

        Scanner input = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String stateString = input.nextLine();
            try {
                var blockState = new BlockState(stateString);
                System.out.printf("%016x %d %s%n", blockState.longValue(), blockState.protocolId(), blockState);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
