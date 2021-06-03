package com.pygostylia.osprey.nbt;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class NBTCompound extends NBTValue implements Map<String, NBTValue> {
    public static int ID = 10;
    Map<String, NBTValue> value = new LinkedHashMap<>();
    String name;

    public NBTCompound(String name) {
        this.name = name;
    }

    public NBTCompound() {
    }

    @Override
    public int id() {
        return ID;
    }

    @Override
    void encode(OutputStream os) throws IOException {
        for (var key : value.keySet()) {
            NBTValue v = value.get(key);
            os.write(v.id());
            Conversion.putModifiedString(os, key);
            v.encode(os);
        }
        (new NBTEnd()).encode(os);
    }

    public void write(OutputStream os) throws IOException {
        os.write(id());
        if (name == null) {
            Conversion.outputShort(os, 0);
        } else {
            Conversion.putModifiedString(os, name);
        }
        encode(os);
    }

    public NBTValue put(String key, byte value) {
        return this.value.put(key, new NBTByte(value));
    }

    public NBTValue put(String key, short value) {
        return this.value.put(key, new NBTShort(value));
    }

    public NBTValue put(String key, int value) {
        return this.value.put(key, new NBTInteger(value));
    }

    public NBTValue put(String key, long value) {
        return this.value.put(key, new NBTLong(value));
    }

    public NBTValue put(String key, float value) {
        return this.value.put(key, new NBTFloat(value));
    }

    public NBTValue put(String key, double value) {
        return this.value.put(key, new NBTDouble(value));
    }

    public NBTValue put(String key, String value) {
        return this.value.put(key, new NBTString(value));
    }

    public NBTValue put(String key, byte[] value) {
        return this.value.put(key, new NBTByteArray(value));
    }

    public NBTValue put(String key, Integer[] value) {
        return this.value.put(key, new NBTIntegerArray(value));
    }

    public NBTValue put(String key, Long[] value) {
        return this.value.put(key, new NBTLongArray(value));
    }

    @Override
    public NBTValue put(String key, NBTValue value) {
        return this.value.put(key, value);
    }

    public byte getByte(String key) {
        if (get(key) instanceof NBTByte nbtByte) {
            return nbtByte.value;
        } else {
            throw new NBTException(key + " is not an NBTByte");
        }
    }

    public short getShort(String key) {
        if (get(key) instanceof NBTShort nbtShort) {
            return nbtShort.value;
        } else {
            throw new NBTException(key + " is not an NBTShort");
        }
    }

    @Override
    public NBTValue remove(Object key) {
        return value.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends NBTValue> m) {
        value.putAll(m);
    }

    @Override
    public void clear() {
        value.clear();
    }

    @Override
    public Set<String> keySet() {
        return value.keySet();
    }

    @Override
    public Collection<NBTValue> values() {
        return value.values();
    }

    @Override
    public Set<Entry<String, NBTValue>> entrySet() {
        return value.entrySet();
    }

    public int size() {
        return value.size();
    }

    @Override
    public boolean isEmpty() {
        return value.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return value.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.value.containsValue(value);
    }

    @Override
    public NBTValue get(Object key) {
        return value.get(key);
    }
}
