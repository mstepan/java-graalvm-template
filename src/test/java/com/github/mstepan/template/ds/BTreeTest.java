package com.github.mstepan.template.ds;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
