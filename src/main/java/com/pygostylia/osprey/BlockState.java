package com.pygostylia.osprey;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BlockState {
    private long state;

    public BlockState(JSONObject registryObject) {}
    public BlockState(int blockId, HashMap<String, Integer> states) {}
    public BlockState(String blockIdString) {}

    public BlockState(long value) {
        state = value;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        var blockId = blockId();
        if (blockName() == null) {
            throw new IllegalStateException("Oops");
        }
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
        return false;
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

    public boolean setRaw(String key, Integer value) {
        return false;
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

    private static record Type(int type) {}
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
        return 32 - Integer.numberOfLeadingZeros(v - 1);
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
            final int blockIdLength = 10;
            final int typeIdLength = 8;
            JSONObject block = blocks.getJSONObject(key);
            var props = block.optJSONObject("properties");

            int offset = blockIdLength + typeIdLength;
            final var thisTypeFields = new HashMap<String, BitField>();
            Type thisType;
            if (props != null) {
                for (var prop : props.keySet()) {
                    var count = props.getJSONArray(prop).length();
                    var bits = log2(count);
                    var values = new HashMap<String, Integer>();
                    int valueN = 0;
                    for (var ovalue : props.getJSONArray(prop)) {
                        String value = (String) ovalue;
                        values.put(value, valueN++);
                    }
                    thisTypeFields.put(prop, new BitField(offset, bits, values));
                    offset += bits;
                    if (offset > 63) System.out.println("uh oh");
                }
            }
            if (blockStateTypes.containsKey(thisTypeFields)) {
                thisType = blockStateTypes.get(thisTypeFields);
            } else {
                thisType = new Type(nextType++);
                blockStateTypes.put(thisTypeFields, thisType);
                typeFields.put(thisType, thisTypeFields);
            }
            var blockId = blockIds.get(key);
            blockTypes.put(blockId, thisType);
            var blockType = blockTypes.get(blockId);
            var fields = typeFields.get(blockType);

            JSONArray states = block.optJSONArray("states");
            for (Object ostate : states) {
                var state = (JSONObject) ostate;
                long internal = 0;
                internal |= blockId;
                internal |= (long) blockType.type() << blockIdLength;
                short stateId = (short) state.getInt("id");
                var properties = state.optJSONObject("properties");
                if (properties != null) {
                    for (var prop : properties.keySet()) {
                        var field = fields.get(prop);
                        var propValue = properties.getString(prop);
                        var propInt = field.values().get(propValue);
                        internal |= (long) propInt << field.offset;
                    }
                }
                internalLongs.put(stateId, internal);
                protocolIds.put(internal, stateId);
            }
        }
        // System.out.println(blockStateTypes);
        // System.out.println(typeFields);
        // System.out.println(blockTypes);
        // System.out.println(internalLongs);

        for (short protocolId = 0; protocolId < 1000; protocolId++) {
            var longValue = internalLongs.get(protocolId);
            var state = new BlockState(internalLongs.get(protocolId));
            System.out.printf("%s %016x %s%n", state.protocolId(), longValue, state);
        }

        for (var x : protocolIds.keySet().stream().sorted().collect(Collectors.toList())) {
            var state = new BlockState(x);
            System.out.printf("%016x %d %s %s%n", x, Long.numberOfLeadingZeros(x), state, state.fields());
        }
    }
}
