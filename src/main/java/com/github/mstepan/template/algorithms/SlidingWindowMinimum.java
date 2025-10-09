package com.github.mstepan.template.algorithms;

import com.github.mstepan.template.ds.SlidingWindow;
import java.util.Objects;

public class SlidingWindowMinimum {

    /**
     * time: O(N)
     *
     * <p>space: O(N-K+1)
     */
    public static int[] slidingWindowMin(int[] arr, int windowSize) {
        Objects.requireNonNull(arr);

        if (arr.length == 0 && windowSize == 0) {
            return new int[] {};
        }

        if (windowSize <= 0 || windowSize > arr.length) {
            throw new IllegalArgumentException(
                    String.format(
                            "Window size must be in range [1, %d], windowSize = %d",
                            arr.length, windowSize));
        }

        int[] res = new int[arr.length - windowSize + 1];

        SlidingWindow window = SlidingWindow.fromArray(arr, windowSize);

        res[0] = window.getMin();

        for (int i = 1; i < res.length; ++i) {
            window.removeLeft(arr[i - 1]);
            window.addRight(arr[i + windowSize - 1]);
            res[i] = window.getMin();
        }

        return res;
    }
}
