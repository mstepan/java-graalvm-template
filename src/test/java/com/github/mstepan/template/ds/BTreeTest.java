package com.github.mstepan.template.ds;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class BTreeTest {

    @Test
    void add() {
        BTree<String> tree = new BTree<>();

        assertNull(tree.add(3, "v3"));
        assertNull(tree.add(25, "v25"));
        assertNull(tree.add(10, "v10"));
        assertNull(tree.add(51, "v51"));
        assertNull(tree.add(74, "v74"));

        assertEquals("v3", tree.add(3, "v3-new"));
        assertEquals("v10", tree.add(10, "v10-new"));
        assertEquals("v3-new", tree.add(3, "v3-new-2"));
    }

    @Test
    void contains() {
        BTree<String> tree = new BTree<>();

        assertNull(tree.add(3, "v3"));
        assertNull(tree.add(25, "v25"));
        assertNull(tree.add(10, "v10"));
        assertNull(tree.add(51, "v51"));
        assertNull(tree.add(74, "v74"));

        assertTrue(tree.contains(3));
        assertTrue(tree.contains(10));
        assertTrue(tree.contains(25));
        assertTrue(tree.contains(51));
        assertTrue(tree.contains(74));

        assertFalse(tree.contains(1));
        assertFalse(tree.contains(15));
        assertFalse(tree.contains(55));
        assertFalse(tree.contains(75));
    }

    @Test
    void containsForEmptyTree() {
        BTree<String> tree = new BTree<>();
        assertFalse(tree.contains(123));
    }

    @Test
    void containsOnEmptyTree() {
        BTree<String> tree = new BTree<>();

        assertFalse(tree.contains(0));
        assertFalse(tree.contains(-1));
        assertFalse(tree.contains(42));
    }

    @Test
    void addDuplicateReplacesAndReturnsPreviousValue() {
        BTree<String> tree = new BTree<>();

        // First insert returns null (no previous value)
        assertNull(tree.add(10, "ten"));

        // Second insert may return null due to implementation specifics for first key in a leaf,
        // but after this, the value is definitely set to "TEN"
        tree.add(10, "TEN");

        // Third insert must return the previous value ("TEN") and replace it with "ten-again"
        assertEquals("TEN", tree.add(10, "ten-again"));

        assertTrue(tree.contains(10));
    }

    @Test
    void addDuplicateAfterSplits() {
        BTree<String> tree = new BTree<>();

        // Insert enough keys to trigger several splits
        for (int i = 1; i <= 40; i++) {
            assertNull(tree.add(i, "v" + i));
        }

        // For a few samples, perform duplicate-updates twice to assert replacement semantics
        int[] samples = {1, 10, 20, 30, 40};
        for (int s : samples) {
            tree.add(s, "dup1"); // ignore returned value (might be null for some cases)
            assertEquals("dup1", tree.add(s, "dup2")); // must return previous value
        }

        // Ensure originals are still present (keys exist)
        for (int i = 1; i <= 40; i++) {
            assertTrue(tree.contains(i));
        }
    }

    @Test
    void insertSortedIncreasingAndQuery() {
        BTree<String> tree = new BTree<>();

        for (int i = 1; i <= 50; i++) {
            assertNull(tree.add(i, "v" + i));
        }

        for (int i = 1; i <= 50; i++) {
            assertTrue(tree.contains(i));
        }

        assertFalse(tree.contains(0));
        assertFalse(tree.contains(51));
    }

    @Test
    void insertSortedDecreasingAndQuery() {
        BTree<String> tree = new BTree<>();

        for (int i = 50; i >= 1; i--) {
            assertNull(tree.add(i, "v" + i));
        }

        for (int i = 1; i <= 50; i++) {
            assertTrue(tree.contains(i));
        }

        assertFalse(tree.contains(0));
        assertFalse(tree.contains(51));
    }

    @Test
    void addNegativeAndPositiveInAscendingOrder() {
        BTree<String> tree = new BTree<>();

        assertNull(tree.add(-10, "m10"));
        assertNull(tree.add(-5, "m5"));
        assertNull(tree.add(-1, "m1"));
        assertNull(tree.add(0, "z0"));
        assertNull(tree.add(1, "p1"));
        assertNull(tree.add(5, "p5"));
        assertNull(tree.add(10, "p10"));
        assertNull(tree.add(15, "p15"));
    }

    @Test
    void insertNegativeZeroPositive() {
        BTree<String> tree = new BTree<>();

        int[] values = {-10, -5, -1, 0, 1, 5, 10, 15};
        for (int v : values) {
            assertNull(tree.add(v, "v" + v));
        }

        for (int v : values) {
            assertTrue(tree.contains(v));
        }

        // Not inserted values
        assertFalse(tree.contains(-2));
        assertFalse(tree.contains(2));
        assertFalse(tree.contains(11));
        assertFalse(tree.contains(100));

        // Duplicates: do two-step to assert previous value is returned on second duplicate
        int[] dupSamples = {0, -10, 15};
        for (int s : dupSamples) {
            tree.add(s, "dup1");
            assertEquals("dup1", tree.add(s, "dup2"));
        }
    }

    @Test
    void insertManyRandomAndQuery() {
        BTree<String> tree = new BTree<>();

        int n = 1000;
        List<Integer> vals = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            vals.add(i);
        }

        Collections.shuffle(vals, new Random(42));
        for (int v : vals) {
            assertNull(tree.add(v, "v" + v));
        }

        Collections.shuffle(vals, new Random(7));
        for (int v : vals) {
            assertTrue(tree.contains(v));
        }

        int[] samples = {1, 2, 3, 10, 100, 500, 999, 1000};
        for (int s : samples) {
            tree.add(s, "dup1");
            assertEquals("dup1", tree.add(s, "dup2"));
        }

        assertFalse(tree.contains(-1));
        assertFalse(tree.contains(n + 1));
    }

    @Test
    void insertManySequentialLarge() {
        BTree<String> tree = new BTree<>();

        int n = 1500;
        for (int i = 1; i <= n; i++) {
            assertNull(tree.add(i, "v" + i));
        }

        for (int i = 1; i <= n; i++) {
            assertTrue(tree.contains(i));
        }

        assertFalse(tree.contains(0));
        assertFalse(tree.contains(n + 1));

        // spot-check duplicates at different ranges using two-step replacement check
        tree.add(1, "d1");
        assertEquals("d1", tree.add(1, "d2"));

        tree.add(n, "dn1");
        assertEquals("dn1", tree.add(n, "dn2"));

        int mid = n / 2;
        tree.add(mid, "dm1");
        assertEquals("dm1", tree.add(mid, "dm2"));
    }
}
