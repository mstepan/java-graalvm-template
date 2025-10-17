package com.github.mstepan.template.ds;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RobinHoodHashMapTest {

    @Test
    void putGetBasic() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        Integer prev = map.put("a", 1);
        assertNull(prev);
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());

        assertEquals(1, map.get("a"));
        assertNull(map.get("b"));
    }

    @Test
    void updateExistingKeyReturnsPreviousValueAndSizeUnchanged() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();

        assertNull(map.put("k", 100));
        assertEquals(1, map.size());
        assertEquals(100, map.get("k"));

        Integer prev = map.put("k", 200);
        assertEquals(100, prev);
        assertEquals(1, map.size());
        assertEquals(200, map.get("k"));
    }

    @Test
    void containsKeyTrueForPresentFalseForAbsent() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();

        map.put("x", 42);

        assertTrue(map.containsKey("x"));
        assertFalse(map.containsKey("y"));
    }

    @Test
    void nullKeyOperationsThrow() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();

        assertThrows(IllegalArgumentException.class, () -> map.put(null, 1));
        assertThrows(IllegalArgumentException.class, () -> map.get(null));
        assertThrows(IllegalArgumentException.class, () -> map.containsKey(null));
    }

    @Test
    void clearResetsMap() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        assertEquals(3, map.size());
        assertFalse(map.isEmpty());

        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get("a"));
        assertNull(map.get("b"));
        assertNull(map.get("c"));

        assertTrue(map.entrySet().isEmpty());
    }

    @Test
    void entrySetContainsAllPairs() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();

        Map<String, Integer> expected = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            String k = "k" + i;
            int v = i * i;
            expected.put(k, v);
            map.put(k, v);
        }

        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(expected.size(), entries.size());

        for (Map.Entry<String, Integer> e : entries) {
            assertTrue(expected.containsKey(e.getKey()));
            assertEquals(expected.get(e.getKey()), e.getValue());
        }
    }

    @Test
    void resizePreservesAllEntries() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();

        // Default capacity is 8, resize triggers when inserting the 7th unique element.
        for (int i = 0; i < 20; i++) {
            assertNull(map.put("key-" + i, i));
        }

        assertEquals(20, map.size());

        for (int i = 0; i < 20; i++) {
            assertTrue(map.containsKey("key-" + i));
            assertEquals(i, map.get("key-" + i));
        }
    }

    @Test
    void handlesManyCollisionsSameHashCode() {
        RobinHoodHashMap<TestKey, String> map = new RobinHoodHashMap<>();

        // Force all keys to collide into the same initial slot cluster (same hash)
        final int sameHash = 42;
        for (int i = 0; i < 25; i++) {
            TestKey k = new TestKey(i, sameHash);
            String v = "v" + i;
            assertNull(map.put(k, v));
        }

        assertEquals(25, map.size());

        for (int i = 0; i < 25; i++) {
            TestKey k = new TestKey(i, sameHash);
            assertTrue(map.containsKey(k));
            assertEquals("v" + i, map.get(k));
        }
    }

    @Test
    void isEmptyTransitions() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();

        assertTrue(map.isEmpty());
        map.put("a", 1);
        assertFalse(map.isEmpty());
        map.put("a", 2); // update should not affect emptiness
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    void removeReturnsNullWhenKeyAbsent() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();
        map.put("a", 1);

        Integer removed = map.remove("b");
        assertNull(removed, "Removing absent key should return null");
        assertEquals(1, map.size(), "Size must be unchanged when removing absent key");
        assertTrue(map.containsKey("a"));
        assertEquals(1, map.get("a"));
    }

    @Test
    void removeExistingKeyUpdatesSizeAndRemovesMapping() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Integer removed = map.remove("a");
        assertEquals(1, removed);
        assertEquals(1, map.size());
        assertFalse(map.containsKey("a"));
        assertNull(map.get("a"));

        assertTrue(map.containsKey("b"));
        assertEquals(2, map.get("b"));
    }

    @Test
    void removeNullKeyThrows() {
        RobinHoodHashMap<String, Integer> map = new RobinHoodHashMap<>();
        assertThrows(IllegalArgumentException.class, () -> map.remove(null));
    }

    @Test
    void removeCompactsClusterMiddleDeletion() {
        RobinHoodHashMap<TestKey, String> map = new RobinHoodHashMap<>();

        final int sameHash = 7;
        TestKey k0 = new TestKey(0, sameHash);
        TestKey k1 = new TestKey(1, sameHash);
        TestKey k2 = new TestKey(2, sameHash);

        assertNull(map.put(k0, "v0"));
        assertNull(map.put(k1, "v1"));
        assertNull(map.put(k2, "v2"));

        assertEquals(3, map.size());

        String removed = map.remove(k1);
        assertEquals("v1", removed);
        assertEquals(2, map.size());

        // Remaining keys must still be reachable and correct
        assertTrue(map.containsKey(k0));
        assertEquals("v0", map.get(k0));

        assertTrue(map.containsKey(k2));
        assertEquals("v2", map.get(k2));

        assertFalse(map.containsKey(k1));
        assertNull(map.get(k1));
    }

    @Test
    void removeCompactsClusterHeadDeletion() {
        RobinHoodHashMap<TestKey, String> map = new RobinHoodHashMap<>();

        final int sameHash = 13;
        TestKey k0 = new TestKey(0, sameHash);
        TestKey k1 = new TestKey(1, sameHash);
        TestKey k2 = new TestKey(2, sameHash);
        TestKey k3 = new TestKey(3, sameHash);

        map.put(k0, "v0");
        map.put(k1, "v1");
        map.put(k2, "v2");
        map.put(k3, "v3");

        assertEquals("v0", map.remove(k0));
        assertEquals(3, map.size());

        assertTrue(map.containsKey(k1));
        assertEquals("v1", map.get(k1));

        assertTrue(map.containsKey(k2));
        assertEquals("v2", map.get(k2));

        assertTrue(map.containsKey(k3));
        assertEquals("v3", map.get(k3));
    }

    @Test
    void removeAfterResizeWithManyCollisions() {
        RobinHoodHashMap<TestKey, String> map = new RobinHoodHashMap<>();

        final int sameHash = 42;
        final int total = 25;

        for (int i = 0; i < total; i++) {
            assertNull(map.put(new TestKey(i, sameHash), "v" + i));
        }
        assertEquals(total, map.size());

        // Remove a few scattered keys
        assertEquals("v0", map.remove(new TestKey(0, sameHash)));
        assertEquals("v13", map.remove(new TestKey(13, sameHash)));
        assertEquals("v24", map.remove(new TestKey(24, sameHash)));
        assertNull(map.remove(new TestKey(24, sameHash))); // removing again returns null

        assertEquals(total - 3, map.size());

        // Verify remaining keys are intact and reachable
        for (int i = 1; i < total - 1; i++) {
            if (i == 13) {
                continue;
            }
            TestKey k = new TestKey(i, sameHash);
            assertTrue(map.containsKey(k), "Expected key " + i + " to remain present");
            assertEquals("v" + i, map.get(k));
        }

        // Removed keys should be absent
        assertFalse(map.containsKey(new TestKey(0, sameHash)));
        assertFalse(map.containsKey(new TestKey(13, sameHash)));
        assertFalse(map.containsKey(new TestKey(24, sameHash)));
    }

    /** Helper key with controlled hashCode to craft collision scenarios. */
    private static final class TestKey {
        private final int id;
        private final int hash;

        TestKey(int id, int hash) {
            this.id = id;
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TestKey other = (TestKey) obj;
            return this.id == other.id;
        }

        @Override
        public String toString() {
            return "TestKey{id=" + id + ", hash=" + hash + "}";
        }
    }
}
