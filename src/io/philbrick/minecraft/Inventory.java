package io.philbrick.minecraft;

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
}
