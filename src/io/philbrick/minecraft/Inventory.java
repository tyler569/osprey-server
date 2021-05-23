package io.philbrick.minecraft;

public class Inventory {
    Slot[] slots;
    final int slotCount = 45;

    Inventory() {
        slots = new Slot[slotCount];
        for (int i = 0; i < slotCount; i++) {
            slots[i] = new Slot();
        }
        slots[36].itemId = 1;
        slots[36].empty = false;
        slots[36].count = 1;
    }

    int size() {
        return slots.length;
    }
}
