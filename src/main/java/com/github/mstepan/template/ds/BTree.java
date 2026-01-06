package com.github.mstepan.template.ds;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class BTree<T> {

    private static final int KEYS_PER_NODE = 3;

    private LevelNode root = new LevelNode();

    @SuppressWarnings("unchecked")
    public T add(int key, T value) {

        final Deque<LevelNode> traversalPath = findLeafNodeForKey(key);
        assert !traversalPath.isEmpty();

        LevelNode candidateNode = traversalPath.pop();
        assert candidateNode.isLeaf();

        int keyIdx = candidateNode.findIndexForKey(key);

        if (keyIdx >= 0) {
            // key found, replace value and return old one
            Object oldValue = candidateNode.values[keyIdx];
            candidateNode.values[keyIdx] = value;

            return (T) oldValue;
        }

        candidateNode.insertKeyAndValue(key, value);

        LevelNode cur = candidateNode;

        while (cur.isFull()) {

            LevelNode parent = traversalPath.isEmpty() ? null : traversalPath.pop();

            SplitInfo splitInfo = cur.split(cur.isLeaf() ? SplitType.LEAF : SplitType.INTERNAL);

            if (parent == null) {
                // root is full, we should split it
                root = new LevelNode();
                parent = root;
            }

            // Insert separator into parent and correctly shift child pointers
            int prevKeys = parent.length;
            int insertIdx = parent.insertKeyAndValue(splitInfo.splitKey, value);

            // shift children to make room for the new right child at insertIdx + 1
            if (prevKeys - insertIdx >= 0) {
                System.arraycopy(
                        parent.children,
                        insertIdx + 1,
                        parent.children,
                        insertIdx + 2,
                        prevKeys - insertIdx);
            }

            parent.children[insertIdx] = splitInfo.left;
            parent.children[insertIdx + 1] = splitInfo.right;

            cur = parent;
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

            int idx = cur.findIndexForKey(key);

            if (idx >= 0) {
                traversalPath.push(cur);
                cur = cur.children[idx + 1];
            } else {
                int insertionPoint = -(idx + 1);
                traversalPath.push(cur);
                cur = cur.children[insertionPoint];
            }
        }

        traversalPath.push(cur);

        return traversalPath;
    }

    private static class LevelNode {

        LevelNode() {
            keys = new int[KEYS_PER_NODE];
            values = new Object[KEYS_PER_NODE];
            length = 0;

            children = new LevelNode[KEYS_PER_NODE + 1];
        }

        LevelNode(int key) {
            this();
            keys[0] = key;
            length++;
        }

        // store 'keys' and 'values' in separate arrays for better locality
        int[] keys;
        Object[] values;
        int length;

        LevelNode[] children;

        public boolean isLeaf() {
            return children[0] == null;
        }

        public int findIndexForKey(int key) {
            return Arrays.binarySearch(keys, 0, length, key);
        }

        public int insertKeyAndValue(int key, Object value) {
            if (length == 0) {
                keys[length] = key;
                values[length] = value;
                ++length;
                return 0;
            }

            int idx = length - 1;

            while (idx >= 0 && keys[idx] >= key) {
                if (keys[idx] == key) {
                    values[idx] = value;
                    return idx;
                }

                keys[idx + 1] = keys[idx];
                --idx;
            }

            keys[idx + 1] = key;
            values[idx + 1] = value;

            ++length;

            return idx + 1;
        }

        @Override
        public String toString() {
            return Arrays.toString(keys) + " => " + (isLeaf() ? "LEAF" : "INTERNAL");
        }

        public boolean isFull() {
            return keys.length == length;
        }

        public SplitInfo split(SplitType type) {
            int mid = length / 2;

            LevelNode left = new LevelNode();
            for (int i = 0; i < mid; ++i) {
                left.keys[i] = keys[i];
                left.values[i] = values[i];
                left.length++;
            }
            // move children pointers
            System.arraycopy(children, 0, left.children, 0, mid + 1);

            LevelNode right = new LevelNode();
            for (int offset = (mid + (type == SplitType.LEAF ? 0 : 1)), i = 0;
                    offset < length;
                    ++offset, ++i) {
                assert i < right.keys.length;

                right.keys[i] = keys[offset];
                right.values[i] = values[offset];
                right.length++;
            }

            // move children pointers
            System.arraycopy(children, mid + 1, right.children, 0, children.length - (mid + 1));

            return new SplitInfo(keys[mid], left, right);
        }
    }

    enum SplitType {
        INTERNAL,
        LEAF,
    }

    private record SplitInfo(int splitKey, LevelNode left, LevelNode right) {}
}
