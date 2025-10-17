package com.github.mstepan.template.ds;

import java.util.AbstractMap;
import java.util.Set;

public class RobinHoodHashMap<K, V> extends AbstractMap<K, V> {

    private static final int DEFAULT_CAPACITY = 8;
    private static final double DEFAULT_LOAD_FACTOR = 0.75;

    private SlotEntry<K, V>[] slots = allocateSlotsArray(DEFAULT_CAPACITY);

    private int size;

    public RobinHoodHashMap() {}

    @Override
    public V put(K initialKey, V initialValue) {
        if (initialKey == null) {
            throw new IllegalArgumentException("key can't be null");
        }

        V prevValue = insertValue(initialKey, initialValue);

        if (prevValue == null) {
            ++size;
        }

        return prevValue;
    }

    private V insertValue(K initialKey, V initialValue) {

        final int slotsMod = slots.length - 1;
        V prevValue = null;

        K key = initialKey;
        V value = initialValue;
        int initialSlotIdx = hash(initialKey) & slotsMod;

        int idx = initialSlotIdx;

        while (true) {
            // empty slot found,m just insert key-value
            if (slots[idx] == null) {
                slots[idx] = new SlotEntry<>(key, value, initialSlotIdx);
                break;
            }

            final SlotEntry<K, V> cur = slots[idx];

            // not empty slot found with teh same 'key', replace value
            if (cur.key.equals(key)) {
                prevValue = cur.value;
                cur.value = value;
                break;
            }

            // swap value
            if (cur.initialSlotIdx > initialSlotIdx) {
                // save key, value, initialSlotIdx for new value
                K tempKey = cur.key;
                V tempValue = cur.value;
                int tempInitialSlotIdx = cur.initialSlotIdx;

                // replace value in-place
                cur.key = key;
                cur.value = value;
                cur.initialSlotIdx = initialSlotIdx;

                // move forward with insertion for swapped value
                key = tempKey;
                value = tempValue;
                initialSlotIdx = tempInitialSlotIdx;
            }

            idx = (idx + 1) & slotsMod;
        }

        final double load = loadFactor();

        if (Double.compare(load, DEFAULT_LOAD_FACTOR) >= 0) {
            resize();
        }

        return prevValue;
    }

    private int hash(K key) {
        return key.hashCode();
    }

    private void resize() {
        final SlotEntry<K, V>[] oldSlots = slots;

        slots = allocateSlotsArray(oldSlots.length * 2);

        for (SlotEntry<K, V> singleOldSlot : oldSlots) {
            if (singleOldSlot != null) {
                insertValue(singleOldSlot.key, singleOldSlot.value);
            }
        }
    }

    private double loadFactor() {
        return ((double) size) / slots.length;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        slots = allocateSlotsArray(DEFAULT_CAPACITY);
        size = 0;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Set.of();
    }

    @SuppressWarnings("unchecked")
    private static <K, V> SlotEntry<K, V>[] allocateSlotsArray(int slotsSize) {
        return (SlotEntry<K, V>[]) new SlotEntry[slotsSize];
    }

    static class SlotEntry<K, V> {
        K key;
        V value;
        int initialSlotIdx;

        SlotEntry(K key, V value, int initialSlotIdx) {
            this.key = key;
            this.value = value;
            this.initialSlotIdx = initialSlotIdx;
        }

        @Override
        public String toString() {
            return String.format("%s=%s, (%d)", key, value, initialSlotIdx);
        }
    }
}
