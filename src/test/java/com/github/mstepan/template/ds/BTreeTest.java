package com.github.mstepan.template.ds;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class BTreeTest {

    @Test
    void add() {
        BTree tree = new BTree();

        assertTrue(tree.add(3));
        assertTrue(tree.add(25));
        assertTrue(tree.add(10));
        assertTrue(tree.add(51));
        assertTrue(tree.add(74));
    }

    @Test
    void contains() {
        BTree tree = new BTree();

        tree.add(3);
        tree.add(25);
        tree.add(10);
        tree.add(51);
        tree.add(74);

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
        BTree tree = new BTree();
        assertFalse(tree.contains(123));
    }

    @Test
    void containsOnEmptyTree() {
        BTree tree = new BTree();

        assertFalse(tree.contains(0));
        assertFalse(tree.contains(-1));
        assertFalse(tree.contains(42));
    }

    @Test
    void addDuplicateReturnsFalse() {
        BTree tree = new BTree();

        assertTrue(tree.add(10));
        assertFalse(tree.add(10)); // duplicate in the same leaf
        assertTrue(tree.contains(10));
    }

    @Test
    void addDuplicateAfterSplits() {
        BTree tree = new BTree();

        // Insert enough keys to trigger several splits
        for (int i = 1; i <= 40; i++) {
            assertTrue(tree.add(i));
        }

        // Attempt to add duplicates after the tree has grown multiple levels
        assertFalse(tree.add(1));
        assertFalse(tree.add(10));
        assertFalse(tree.add(20));
        assertFalse(tree.add(30));
        assertFalse(tree.add(40));

        // Ensure originals are still present
        for (int i = 1; i <= 40; i++) {
            assertTrue(tree.contains(i));
        }
    }

    @Test
    void insertSortedIncreasingAndQuery() {
        BTree tree = new BTree();

        for (int i = 1; i <= 50; i++) {
            assertTrue(tree.add(i));
        }

        for (int i = 1; i <= 50; i++) {
            assertTrue(tree.contains(i));
        }

        assertFalse(tree.contains(0));
        assertFalse(tree.contains(51));
    }

    @Test
    void insertSortedDecreasingAndQuery() {
        BTree tree = new BTree();

        for (int i = 50; i >= 1; i--) {
            assertTrue(tree.add(i));
        }

        for (int i = 1; i <= 50; i++) {
            assertTrue(tree.contains(i));
        }

        assertFalse(tree.contains(0));
        assertFalse(tree.contains(51));
    }

    @Test
    void addNegativeAndPositiveInAscendingOrder() {
        BTree tree = new BTree();

        assertTrue(tree.add(-10));
        assertTrue(tree.add(-5));
        assertTrue(tree.add(-1));
        assertTrue(tree.add(0));
        assertTrue(tree.add(1));
        assertTrue(tree.add(5));
        assertTrue(tree.add(10));
        assertTrue(tree.add(15));
    }

    @Test
    void insertNegativeZeroPositive() {
        BTree tree = new BTree();

        int[] values = {-10, -5, -1, 0, 1, 5, 10, 15};
        for (int v : values) {
            assertTrue(tree.add(v));
        }

        for (int v : values) {
            assertTrue(tree.contains(v));
        }

        // Not inserted values
        assertFalse(tree.contains(-2));
        assertFalse(tree.contains(2));
        assertFalse(tree.contains(11));
        assertFalse(tree.contains(100));

        // Duplicates across sign boundaries
        assertFalse(tree.add(0));
        assertFalse(tree.add(-10));
        assertFalse(tree.add(15));
    }

    @Test
    void insertManyRandomAndQuery() {
        BTree tree = new BTree();

        int n = 1000;
        List<Integer> vals = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            vals.add(i);
        }

        Collections.shuffle(vals, new Random(42));
        for (int v : vals) {
            assertTrue(tree.add(v));
        }

        Collections.shuffle(vals, new Random(7));
        for (int v : vals) {
            assertTrue(tree.contains(v));
        }

        int[] samples = {1, 2, 3, 10, 100, 500, 999, 1000};
        for (int s : samples) {
            assertFalse(tree.add(s));
        }

        assertFalse(tree.contains(-1));
        assertFalse(tree.contains(n + 1));
    }

    @Test
    void insertManySequentialLarge() {
        BTree tree = new BTree();

        int n = 1500;
        for (int i = 1; i <= n; i++) {
            assertTrue(tree.add(i));
        }

        for (int i = 1; i <= n; i++) {
            assertTrue(tree.contains(i));
        }

        assertFalse(tree.contains(0));
        assertFalse(tree.contains(n + 1));

        // spot-check duplicates at different ranges
        assertFalse(tree.add(1));
        assertFalse(tree.add(n));
        assertFalse(tree.add(n / 2));
    }
}
