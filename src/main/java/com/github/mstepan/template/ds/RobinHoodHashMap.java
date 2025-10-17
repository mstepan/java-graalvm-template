package com.github.mstepan.template.ds;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Robin-Hood hashtable implementation. <a
 * href="https://www.cs.cornell.edu/courses/JavaAndDS/files/hashing_RobinHood.pdf">...</a>
 *
 * <p>Instead of 'probe sequence length' we are using 'initial slot index'
 * (SlotEntry.initialSlotIdx)
 */
public final class RobinHoodHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {

    private static final int DEFAULT_CAPACITY = 8;
    private static final double DEFAULT_LOAD_FACTOR = 0.75;

    private SlotEntry<K, V>[] slots = allocateSlotsArray(DEFAULT_CAPACITY);

    private int size;

    public RobinHoodHashMap() {}

    public RobinHoodHashMap(Map<? extends K, ? extends V> otherMap) {
        if (otherMap == null) {
            throw new IllegalArgumentException("'otherMap' is null");
        }

        for (Map.Entry<? extends K, ? extends V> e : otherMap.entrySet()) {
            K key = e.getKey();
            V value = e.getValue();
            put(key, value);
        }
    }

    @Override
    public V put(K initialKey, V initialValue) {
        checkForNullKey(initialKey);

        V prevValue = insertValue(initialKey, initialValue);

        if (prevValue == null) {
            ++size;
        }

        return prevValue;
    }

    @Override
    public V get(Object key) {
        checkForNullKey(key);

        int idx = findEntryIndex(key);
        return (idx == -1) ? null : slots[idx].value;
    }

    private int findEntryIndex(Object key) {

        final int slotsMod = slots.length - 1;

        final int keyInitialSlotIndex = hash(key) & slotsMod;

        int slotIdx = keyInitialSlotIndex;
        SlotEntry<K, V> cur = slots[slotIdx];

        while (cur != null) {

            // return from loop earlier if we found entry with initialSlotIdx > keyInitialSlotIndex
            if (cur.initialSlotIdx > keyInitialSlotIndex) {
                return -1;
            }

            if (cur.key.equals(key)) {
                return slotIdx;
            }

            slotIdx = (slotIdx + 1) & slotsMod;
            cur = slots[slotIdx];
        }

        return -1;
    }

    @Override
    public boolean containsKey(Object key) {
        checkForNullKey(key);

        return findEntryIndex(key) != -1;
    }

    @Override
    public V remove(Object key) {
        checkForNullKey(key);

        final int idx = findEntryIndex(key);

        if (idx == -1) {
            return null;
        }
        final V retValue = slots[idx].value;
        slots[idx] = null;
        --size;

        final int slotsMod = slots.length - 1;

        int prevIdx = idx;

        for (int curIdx = (prevIdx + 1) & slotsMod;
                slots[curIdx] != null;
                curIdx = (curIdx + 1) & slotsMod) {

            if (slots[curIdx].initialSlotIdx == curIdx) {
                break;
            }

            slots[prevIdx] = slots[curIdx];
            slots[curIdx] = null;

            prevIdx = curIdx;
        }

        return retValue;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        slots = allocateSlotsArray(DEFAULT_CAPACITY);
        size = 0;
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> result = new HashSet<>(size);
        for (SlotEntry<K, V> slotEntry : slots) {
            if (slotEntry != null) {
                result.add(new SimpleEntry<>(slotEntry.key, slotEntry.value));
            }
        }

        return result;
    }

    private void checkForNullKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("key can't be null");
        }
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
                cur.replaceValues(key, value, initialSlotIdx);

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

    private int hash(Object key) {
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

        public void replaceValues(K key, V value, int initialSlotIdx) {
            this.key = key;
            this.value = value;
            this.initialSlotIdx = initialSlotIdx;
        }
    }
}
