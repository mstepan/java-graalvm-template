package com.github.mstepan.template.ds;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

}
