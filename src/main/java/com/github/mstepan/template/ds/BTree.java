package com.github.mstepan.template.ds;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class BTree<T> {

    private static final int KEYS_PER_NODE = 13;

    private LevelNode root = new LeafNode();

    @SuppressWarnings("unchecked")
    public T add(int key, T value) {

        final Deque<LevelNode> traversalPath = findLeafNodeForKey(key);
        assert !traversalPath.isEmpty();

        LevelNode candidateNode = traversalPath.pop();
        assert candidateNode.isLeaf();

        LeafNode leaf = (LeafNode) candidateNode;

        int keyIdx = leaf.findIndexForKey(key);

        if (keyIdx >= 0) {
            // key found, replace value and return old one
            Object oldValue = leaf.values[keyIdx];
            leaf.values[keyIdx] = value;

            return (T) oldValue;
        }

        leaf.insertKeyAndValue(key, value);

        LevelNode cur = leaf;

        while (cur.isFull()) {

            LevelNode parent = traversalPath.isEmpty() ? null : traversalPath.pop();

            SplitInfo splitInfo = cur.split();

            if (parent == null) {
                // root is full, we should split it
                root = new InternalNode();
                parent = root;
            }

            // Insert separator into parent and correctly shift child pointers
            InternalNode p = (InternalNode) parent;

            int prevKeys = p.length;
            int insertIdx = p.insertKey(splitInfo.splitKey);

            // shift children to make room for the new right child at insertIdx + 1
            if (prevKeys - insertIdx >= 0) {
                System.arraycopy(
                        p.children, insertIdx + 1, p.children, insertIdx + 2, prevKeys - insertIdx);
            }

            p.children[insertIdx] = splitInfo.left;
            p.children[insertIdx + 1] = splitInfo.right;

            cur = p;
        }
        return null;
    }

    public boolean contains(int key) {
        Deque<LevelNode> traversalPath = findLeafNodeForKey(key);
        assert !traversalPath.isEmpty();

        LevelNode candidateNode = traversalPath.pop();

        int idx = candidateNode.findIndexForKey(key);

        return idx >= 0;
    }

    private Deque<LevelNode> findLeafNodeForKey(int key) {

        Deque<LevelNode> traversalPath = new ArrayDeque<>();

        LevelNode cur = root;

        while (!cur.isLeaf()) {
            InternalNode in = (InternalNode) cur;

            int idx = in.findIndexForKey(key);

            if (idx >= 0) {
                traversalPath.push(in);
                cur = in.children[idx + 1];
            } else {
                int insertionPoint = -(idx + 1);
                traversalPath.push(in);
                cur = in.children[insertionPoint];
            }
        }

        traversalPath.push(cur);

        return traversalPath;
    }

    private abstract static class LevelNode {

        // store 'keys' in a separate array for better locality
        int[] keys;
        int length;

        LevelNode() {
            keys = new int[KEYS_PER_NODE];
            length = 0;
        }

        public abstract SplitInfo split();

        public abstract boolean isLeaf();

        public int findIndexForKey(int key) {
            return Arrays.binarySearch(keys, 0, length, key);
        }

        public boolean isFull() {
            return keys.length == length;
        }

        @Override
        public String toString() {
            return Arrays.toString(keys) + " => " + (isLeaf() ? "LEAF" : "INTERNAL");
        }
    }

    private static class LeafNode extends LevelNode {

        Object[] values;

        LeafNode() {
            super();
            values = new Object[KEYS_PER_NODE];
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        public void insertKeyAndValue(int key, Object value) {
            if (length == 0) {
                keys[length] = key;
                values[length] = value;
                ++length;
                return;
            }

            int idx = length - 1;

            while (idx >= 0 && keys[idx] >= key) {
                if (keys[idx] == key) {
                    values[idx] = value;
                    return;
                }

                keys[idx + 1] = keys[idx];
                values[idx + 1] = values[idx];
                --idx;
            }

            keys[idx + 1] = key;
            values[idx + 1] = value;

            ++length;
        }

        @Override
        public SplitInfo split() {
            int mid = length / 2;

            LeafNode left = new LeafNode();
            for (int i = 0; i < mid; ++i) {
                left.keys[i] = keys[i];
                left.values[i] = values[i];
                left.length++;
            }

            LeafNode right = new LeafNode();
            for (int offset = mid, i = 0; offset < length; ++offset, ++i) {
                right.keys[i] = keys[offset];
                right.values[i] = values[offset];
                right.length++;
            }

            return new SplitInfo(keys[mid], left, right);
        }
    }

    private static class InternalNode extends LevelNode {

        LevelNode[] children;

        InternalNode() {
            super();
            children = new LevelNode[KEYS_PER_NODE + 1];
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        public int insertKey(int key) {
            if (length == 0) {
                keys[length] = key;
                ++length;
                return 0;
            }

            int idx = length - 1;

            while (idx >= 0 && keys[idx] >= key) {
                if (keys[idx] == key) {
                    // duplicate key in internal node is not expected in B-Tree, but keep stable
                    return idx;
                }

                keys[idx + 1] = keys[idx];
                --idx;
            }

            keys[idx + 1] = key;
            ++length;

            return idx + 1;
        }

        @Override
        public SplitInfo split() {
            int mid = length / 2;

            InternalNode left = new InternalNode();
            for (int i = 0; i < mid; ++i) {
                left.keys[i] = keys[i];
                left.length++;
            }
            // move children pointers for left: 0 .. mid (inclusive) - that's mid + 1 children
            System.arraycopy(children, 0, left.children, 0, mid + 1);

            InternalNode right = new InternalNode();
            // keys to right: from mid + 1 .. length - 1
            for (int offset = mid + 1, i = 0; offset < length; ++offset, ++i) {
                right.keys[i] = keys[offset];
                right.length++;
            }
            // move children pointers for right: from mid + 1 .. length (there are length - mid
            // children)
            System.arraycopy(children, mid + 1, right.children, 0, length - mid);

            // promote keys[mid]
            return new SplitInfo(keys[mid], left, right);
        }
    }

    private record SplitInfo(int splitKey, LevelNode left, LevelNode right) {
    }
}
