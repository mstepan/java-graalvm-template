package com.github.mstepan.template.ds;

import java.util.ArrayDeque;
import java.util.Deque;

public final class SlidingWindow {

    public static SlidingWindow fromArray(int[] arr, int windowSize) {

        SlidingWindow windows = new SlidingWindow();
        for (int i = 0; i < windowSize; ++i) {
            windows.addRight(arr[i]);
        }

        return windows;
    }

    final Deque<Integer> deque = new ArrayDeque<>();

    public void addRight(int value) {
        while (!deque.isEmpty() && deque.peekLast() > value) {
            deque.removeLast();
        }

        deque.addLast(value);
    }

    public void removeLeft(int value) {
        assert !deque.isEmpty();

        if (value == deque.peekFirst()) {
            deque.removeFirst();
        }
    }

    public int getMin() {
        if (deque.isEmpty()) {
            throw new IllegalStateException("SlidingWindow is empty");
        }
        return deque.peekFirst();
    }
}
