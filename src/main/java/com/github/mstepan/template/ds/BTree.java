package com.github.mstepan.template.ds;

import java.util.Arrays;

public class BTree {

    private static final int KEYS_PER_NODE = 3;

    private LevelNode root = new LevelNode();

    public boolean add(int key) {
        LevelNode candidateNode = findLeafNodeForKey(key);

        assert candidateNode.isLeaf();

        if (candidateNode.findIndexForKey(key) >= 0) {
            return false;
        }

        candidateNode.insertKey(key);

        LevelNode cur = candidateNode;

        while (cur.isFull()) {

            LevelNode parent = cur.parent;

            if (parent == null) {
                // splitting root
                SplitInfo splitInfo = cur.split(SplitType.ORDINARY);

                root = new LevelNode(splitInfo.splitKey);
                cur = root;
            } else {
                // splitting non root
                SplitInfo splitInfo = cur.split(SplitType.ORDINARY);

                cur.parent = null;

                splitInfo.left.parent = parent;
                splitInfo.right.parent = parent;

                int insertIdx = parent.insertKey(splitInfo.splitKey);
                parent.children[insertIdx] = splitInfo.left;
                parent.children[insertIdx + 1] = splitInfo.right;

                cur = parent;

            }


        }
        return true;

    }

    public boolean contains(int key) {
        LevelNode candidateNode = findLeafNodeForKey(key);

        int idx = candidateNode.findIndexForKey(key);

        return idx >= 0;
    }

    private LevelNode findLeafNodeForKey(int key) {
        LevelNode curNode = root;

        while (!curNode.isLeaf()) {
            int idx = curNode.findIndexForKey(key);

            if (idx >= 0) {
                curNode = curNode.children[idx + 1];
            } else {
                int insertionPoint = -(idx + 1);
                curNode = curNode.children[insertionPoint];
            }
        }

        return curNode;
    }

    private static class LevelNode {

        LevelNode() {
            keys = new int[KEYS_PER_NODE];
            keysLength = 0;

            children = new LevelNode[KEYS_PER_NODE + 1];
        }

        LevelNode(int key) {
            this();
            keys[0] = key;
            keysLength++;
        }


        int[] keys;
        int keysLength;

        LevelNode[] children;

        LevelNode parent;

        public boolean isLeaf() {
            return children[0] == null;
        }

        public int findIndexForKey(int key) {
            return Arrays.binarySearch(keys, 0, keysLength, key);
        }

        public int insertKey(int key) {
            if (keysLength == 0) {
                keys[keysLength] = key;
                ++keysLength;
                return 0;
            }


            int idx = keysLength - 1;

            while (idx >= 0 && keys[idx] >= key) {
                if (keys[idx] == key) {
                    return idx;
                }

                keys[idx + 1] = keys[idx];
                --idx;
            }

            keys[idx + 1] = key;

            ++keysLength;

            return idx + 1;
        }

        @Override
        public String toString() {
            return Arrays.toString(keys);
        }

        public boolean isFull() {
            return keys.length == keysLength;
        }

        public SplitInfo split(SplitType type) {
            int mid = keysLength / 2;

            LevelNode left = new LevelNode();
            for (int i = 0; i < mid; ++i) {
                left.keys[i] = keys[i];
                left.keysLength++;
            }
            System.arraycopy(children, 0, left.children, 0, mid + 1);

            LevelNode right = new LevelNode();
            for (int offset = mid + (type == SplitType.ROOT ? 1 : 0), i = 0; offset < keysLength; ++offset, ++i) {
                assert i < right.keys.length;

                right.keys[i] = keys[offset];
                right.keysLength++;
            }

            System.arraycopy(children, mid + 1, right.children, 0, children.length - (mid + 1));

            return new SplitInfo(keys[mid], left, right);
        }
    }

    enum SplitType {
        ROOT,
        ORDINARY,
    }

    private record SplitInfo(int splitKey, LevelNode left, LevelNode right) {
    }

}
