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
